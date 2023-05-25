package com.idn.rule.mycustomer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.QueryOptions;

public class LifecycleStateIdentityAttributeRuleTest {
	
	@Mock
	SailPointContext context;
	
	@Rule 
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@Spy
	LifecycleStateIdentityAttributeRule rule = new LifecycleStateIdentityAttributeRule();
	
	Identity identity;
		
	@Before
	public void setup() {
		identity = new Identity();
		identity.setName( "test.smith");
		
		rule.identity = identity;
		rule.context = context;
	}
	
	private void simulateSearchLinkObjects(String employeeStatus, String hireDate) throws Exception {
		Filter appFilter = Filter.eq("application.cloudDisplayName", "Workday");
		Filter identityFilter = Filter.eq("identity.name", identity.getName());
		Filter filter = Filter.and(identityFilter, appFilter);
		QueryOptions ops = new QueryOptions();
		ops.addFilter(filter);
		
		String nativeIdentity = "test";
		Attributes attributes = new Attributes();
		attributes.put("HIRE_DATE", hireDate);
		attributes.put("CF_EvalExp_Employee_Status__c", employeeStatus);
		
		Object[] array = new Object[2];
		array[0] = nativeIdentity;
		array[1] = attributes;
		
		List<Object[]> lst = new ArrayList<Object[]>();
		lst.add(array);
		
		when(context.search(Link.class, ops, "nativeIdentity, attributes")).thenReturn(lst.iterator());
	}
	
	private String getDateInSomeDays(int days) {
		Calendar date = Calendar.getInstance();
		date.add(Calendar.DAY_OF_MONTH, days);
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		
		return sdf.format(date.getTime());
	}

	@Test
	public void testOnExtendedLeaver() throws Exception {
		this.simulateSearchLinkObjects("L", "12/30/2019");
		String lcState = rule.executeRule();
		assertEquals("onExtendedLeave", lcState);
	}
	
	//If the End Date is less than 5 days in the past, return suspended as the lifecycle state
	@Test
	public void testSuspendedCase1() throws Exception {
		String endDate = getDateInSomeDays(-2);
		identity.setAttribute("endDate", endDate);
		simulateSearchLinkObjects("P", "12/30/2019");
		String lcState = rule.executeRule();
		
		assertEquals("suspended", lcState);
	}
	
	//If the End date is more than 5 days in the past and the oldValue is not "suspended", return suspended as the lifecycle state
	@Test
	public void testSuspendedCase2() throws Exception {
		String endDate = getDateInSomeDays(-20);
		identity.setAttribute("endDate", endDate);
		simulateSearchLinkObjects("T", "12/30/2019");
		String lcState = rule.executeRule();
		
		assertEquals("suspended", lcState);
	}
	
	//If the End Date is more than 5 days in the past and the oldValue IS "suspended", return inactive as the lifecycle state
	@Test
	public void testSuspendedCase3() throws Exception {
		String endDate = getDateInSomeDays(-20);
		identity.setAttribute("endDate", endDate);
		rule.oldValue = "suspended";
		
		simulateSearchLinkObjects("A", "12/30/2019");
		String lcState = rule.executeRule();
		
		assertEquals("inactive", lcState);
	}
	
	//If the Start Date - 1 day is in the future, return preemployee as the lifecycle state
	@Test
	public void testPreemployee() throws Exception {
		String startDate = getDateInSomeDays(7);
		identity.setAttribute("startDate", startDate);
		
		simulateSearchLinkObjects("A", "12/30/2019");
		String lcState = rule.executeRule();
		
		assertEquals("preemployee", lcState);
	}
	
	//If the Start Date - 1 is in the past, return active as the lifecycle state
	@Test
	public void testActive() throws Exception {
		String startDate = getDateInSomeDays(-60);
		identity.setAttribute("startDate", startDate);
		
		simulateSearchLinkObjects("A", "12/30/2019");
		String lcState = rule.executeRule();
		
		assertEquals("active", lcState);
	}

}
