package com.idn.rule.mycustomer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;

import com.sailpoint.sail4j.annotation.IgnoredBySailPointRule;
import com.sailpoint.sail4j.annotation.SailPointRule;
import com.sailpoint.sail4j.annotation.SailPointRule.RuleType;
import com.sailpoint.sail4j.annotation.SailPointRuleMainBody;

import sailpoint.api.SailPointContext;
import sailpoint.object.Application;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;

@SailPointRule(name = "Generate Username", type = RuleType.ATTRIBUTE_GENERATOR, subFolder = "/MyCustomer/mycustomer-test.com.au")
public class UserNameAttributeGeneratorRule {
	
	@IgnoredBySailPointRule
	Log log;
	
	@IgnoredBySailPointRule
	SailPointContext context;
	
	@IgnoredBySailPointRule
	Application application;
	
	@IgnoredBySailPointRule
	Identity identity;
	
	int maxIteration = 1000;
	
	public String generateUsername ( String firstName,  String lastName, int iteration ) throws GeneralException {

	  // Data protection.
	  firstName = StringUtils.trimToNull( firstName );
	  lastName = StringUtils.trimToNull( lastName );

	  if ( ( firstName == null ) || ( lastName == null ) )
	  return null;

	  // This will hold the final username;
	  String username = null;

	  switch ( iteration ) {
	    case 0:
	      username = firstName + "." + lastName;
	      break;
	    default:
	      username = firstName + "." + lastName + ( iteration - 1 );
	      break;
	  }
	  if ( isUnique ( username ) )
	    return username;
	  else if ( iteration < maxIteration )
	    return generateUsername ( firstName,  lastName, ( iteration + 1 ) );
	  else
	    return null;
	}

	public boolean isUnique ( String username ) throws GeneralException {
	  QueryOptions ops = new QueryOptions();
	  ops.addFilter( Filter.ignoreCase( Filter.eq( "displayName", username ) ) );
	  ops.addFilter( Filter.eq( "application", application ) );
	  int numberFound = context.countObjects( Link.class, ops );
	  return numberFound == 0;
	}
	
	@SailPointRuleMainBody
	public String executeRule() throws GeneralException {
		
		return generateUsername( identity.getFirstname(), identity.getLastname(), 0 );
		
	}

}
