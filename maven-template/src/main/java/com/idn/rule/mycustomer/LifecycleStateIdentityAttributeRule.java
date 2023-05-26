package com.idn.rule.mycustomer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;

import org.apache.commons.logging.Log;

import com.sailpoint.sail4j.annotation.IgnoredBySailPointRule;
import com.sailpoint.sail4j.annotation.SailPointRule;
import com.sailpoint.sail4j.annotation.SailPointRule.RuleType;
import com.sailpoint.sail4j.annotation.SailPointRuleMainBody;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.QueryOptions;

/**
 * This is the Identity Lifecycle State Identity Attribute generation rule for Bupa
 * This rule works as follows:
 * 1. Find all Links associated with the Workday source in the context object
 * 2. Determine which Link is the correct one to use for attributes
 * 2.1 Get the latest one based on Hire Date
 * 3. Validate the Employee Status attribute then decide what to do
 * 3.1 If the Employee Status is L, return onExtendedLeave as the lifecycle state
 * 3.3 If the Employee Status is P, T or A, analyse the Identity Attributes for the lifecycle state
 * 3.3.1 If the End Date is less than 5 days in the past, return suspended as the lifecycle state
 * 3.3.2 If the End date is more than 5 days in the past and the oldValue is not "suspended", return suspended as the lifecycle state
 * 3.3.3 If the End Date is more than 5 days in the past and the oldValue IS "suspended", return inactive as the lifecycle state
 * 3.3.4 If the Start Date - 1 day is in the future, return preemployee as the lifecycle state
 * 3.3.5 If the Start Date - 1 is in the past, return active as the lifecycle state
 * @exception DateTimeParseException When any of the date attributes are unable to be parsed
 * @exception ClassCastException An attribute was of an unexpected Class
 * @exception NullPointerException If there was an unexpected null value in one of the attributes
 * @exception RunTimeException General error caused by values not being from an expected range
 * @return the lifecycle state determined by the script from the list of: preemployee, active, onExtendedLeave, suspended, inactive
 * @author Daniel Griffiths 
 * @version 1.2
 * @since 2020-06-01
 */
@SailPointRule(name = "Lifecycle State", type = RuleType.IDENTITY_ATTRIBUTE, subFolder = "/MyCustomer/mycustomer-test.com.au")
public class LifecycleStateIdentityAttributeRule {
	@IgnoredBySailPointRule
	Log log;
	
	@IgnoredBySailPointRule
	SailPointContext context;
	
	@IgnoredBySailPointRule
	Identity identity;
	
	@IgnoredBySailPointRule
	String oldValue;
	
	// Declaring constants
	String workdaySourceName = "Workday";
	String employeeStatusLabel = "CF_EvalExp_Employee_Status__c";
	String wdHireDateLabel = "HIRE_DATE";
	String timeZoneIdLabel = "Australia/Sydney";
	String wdHireDateFormatString = "MM/dd/yyyy";
	String startDateFormatString = "dd/MM/yyyy";
	String identityAttributeDateFormatString = "dd/MM/yyyy";
	String workdayDateFormatString = "MM/dd/yyyy";
	
	/**
	 * This method will verify whether the user is beyond their end date.  If they are within 5 days
	 * of the End Date Identity Attribute (judged by LDOW/TERMINATION_DATE) they become suspended otherwise
	 * they become inactive
	 * @return String either "suspended", "inactive", or "indate"
	 * @exception DateTimeParseException the Identity Attribute date format is not correct
	 * @exception ClassCastException the date value is not a String and can't be correctly turned into a Date object
	 */
	String endDateStatus() throws Exception {
	    String endDateLabel = "endDate";
	    Object endDateObj = identity.getAttribute(endDateLabel);

	    if (endDateObj != null) {
	        if(endDateObj instanceof String) {
	            String endDateStr = (String)identity.getAttribute(endDateLabel);
	            ZonedDateTime endDateTime = null;
	            DateTimeFormatter identityDateFormatter = DateTimeFormatter.ofPattern(identityAttributeDateFormatString);
	            ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of(timeZoneIdLabel));

	            if(!endDateStr.isEmpty()) {
	                try {
	                    // For terminations it should be at the end of the day
	                    LocalDateTime endDateLocalTime = LocalDate.parse(endDateStr, identityDateFormatter).atTime(23,59);
	                    endDateTime = endDateLocalTime.atZone(ZoneId.of(timeZoneIdLabel));
	                } catch(DateTimeParseException pe) {
	                    // The endDate couldn't be parsed, probably because it's in an incorrect format
	                    throw new DateTimeParseException("Could not parse endDate Identity Attribute with value " + endDateStr, endDateStr, pe.getErrorIndex());
	                }

	                // inactiveDateTime is 5 days after the endDate
	                ZonedDateTime inactiveDateTime = endDateTime.plusDays(5);
	                if(currentDateTime.isAfter(endDateTime)) {
	                    if(currentDateTime.isBefore(inactiveDateTime)) {
	                        // The person has endDated for less than 5 days
	                        return "suspended";
	                    }
	                    // If the existing status was anything other than suspended, catch the state here and return suspended
	                    // this guarantees that the user will never go from an active state directly to inactive as the Inactive state
	                    // requires the accounts to be Disabled before functioning
	                    if(oldValue == null ||
	                        !(oldValue instanceof String) || 
	                        !(
	                                ((String)oldValue).equals("suspended") || ((String)oldValue).equals("inactive")
	                        )
	                        ) {
	                        return "suspended";
	                    }
	                    // Unless it's in the suspended period this person is inactive
	                    return "inactive";
	                }
	            }
	        } else {
	            throw new ClassCastException("endDate Identity Attribute should be a String");
	        }
	    }
	    return "indate";
	}

	/**
	 * This method is used to analyse the Identity attributes of the context's identity object by comparing the current time
	 * to the startDate, endDate, and suspendedDate to determine the lifecycle state of the identity
	 * @return String this is the current state of the identity object based on the startDate and endDate Identity Attributes
	 * @exception DateTimeParseException the Identity Attribute date format is not correct
	 * @exception ClassCastException the date value is not a String and can't be correctly turned into a Date object
	 * @exception NullPointerException startDate is null (endDate is allowed to be null)
	 */
	String dateAnalysis() throws Exception {

	    String endDateStatus = endDateStatus();

	    if(endDateStatus.equals("indate")) {

	        String startDateLabel = "startDate";

	        Object startDateObj = identity.getAttribute(startDateLabel);    
	        
	        // If the startDate attribute doesn't return a value or returns one that cannot be cast to a String throw an exception
	        if(startDateObj != null && (startDateObj instanceof String)) {
	            String startDateStr = (String)startDateObj;
	            
	            ZonedDateTime startDateTime = null;
	            DateTimeFormatter identityDateFormatter = DateTimeFormatter.ofPattern(identityAttributeDateFormatString);
	            
	            try {
	                // Create a LocalDateTime out of the Date string then set the timezone
	                LocalDateTime startDateLocalTime = LocalDate.parse(startDateStr, identityDateFormatter).atStartOfDay();
	                startDateTime = startDateLocalTime.atZone(ZoneId.of(timeZoneIdLabel));
	            } catch(DateTimeParseException pe) {
	                // Parse Exception thrown while attempting to parse the date, usually means unexpected format
	                throw new DateTimeParseException("Could not parse startDate Identity Attribute with value " + startDateStr, startDateStr, pe.getErrorIndex());
	            }

	            // Create a new DateTime object for the day before start date
	            ZonedDateTime startDateTimeMinusOne = startDateTime.minusDays(1);
	            ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of(timeZoneIdLabel));

	            // Check if the current time is after midnight the morning of the day before the start date
	            if (currentDateTime.isAfter(startDateTimeMinusOne)) {
	                // They are past their startDate - 1 and none of the termination effects of endDate apply so the are active
	                return "active";
	            } else {

	                // The startDate is >1 day away, therefore the employee is in preemployee status
	                return "preemployee";
	            }
	        } else {
	            // The startDate is either null or not a String value
	            if(startDateObj != null) {
	                throw new ClassCastException("startDate Identity Attribute should be a String");
	            }
	            throw new NullPointerException("startDate Identity Attribute was null");
	        }
	    } else {
	        return endDateStatus;
	    }
	}
	
	@SailPointRuleMainBody
	public String executeRule() throws Exception {
		// Search for Workday source, return nativeIdentity and attribute list for each linked account in the source
		Filter appFilter = Filter.eq("application.cloudDisplayName", workdaySourceName);
		Filter identityFilter = Filter.eq("identity.name", identity.getName());
		Filter filter = Filter.and(identityFilter, appFilter);
		QueryOptions ops = new QueryOptions();
		ops.addFilter(filter);

		Iterator links = context.search(Link.class, ops, "nativeIdentity, attributes");

		ZonedDateTime newestHireDate = null;
		String latestIdentity = null;
		Attributes latestAttributes = null;
		if (links != null && links.hasNext()) {
		    Object[] row = (Object[]) links.next();
		    Object nativeIdentityObj = row[0];
		    Object attributesObj = row[1];

		    if (attributesObj != null) {
		        if (nativeIdentityObj != null) {
		            String nativeIdentity = (String) nativeIdentityObj;
		            Attributes attributes = (Attributes) attributesObj;

		            Object wdHireDateObj =  attributes.getString("HIRE_DATE");
		            if (wdHireDateObj != null) {
		                String wdHireDate = (String)wdHireDateObj;

		                ZonedDateTime hireDateTime = null;
		                DateTimeFormatter workdayDateFormatter = DateTimeFormatter.ofPattern(workdayDateFormatString);
		                try {
		                    LocalDateTime hireDateLocalTime = LocalDate.parse(wdHireDate, workdayDateFormatter).atStartOfDay();
		                    hireDateTime = hireDateLocalTime.atZone(ZoneId.of(timeZoneIdLabel));
		                } catch(DateTimeParseException pe) {
		                    throw new DateTimeParseException("HIRE_DATE for Workday account " + nativeIdentity + " was not MM/dd/yyyy: " + wdHireDate, wdHireDate, pe.getErrorIndex());
		                }

		                if (newestHireDate == null || hireDateTime.isAfter(newestHireDate)) {
		                    newestHireDate = hireDateTime;
		                    latestIdentity = nativeIdentity;
		                    latestAttributes = attributes;
		                }
		            } else {
		                if(latestIdentity == null && latestAttributes == null) {
		                    latestIdentity = nativeIdentity;
		                    latestAttributes = attributes;
		                }
		            }
		        }
		    }
		}

		String wdEmployeeStatus = latestAttributes.getString(employeeStatusLabel);

		switch(wdEmployeeStatus) {
		    case "L":
		        return "onExtendedLeave";
		    case "T":
		    case "P":
		    case "A":
		        return dateAnalysis();
		    default:
		        throw new RuntimeException(employeeStatusLabel + " value " + wdEmployeeStatus + " is not from the valid set (T,L,P,A) for ");
		}
	}

}
