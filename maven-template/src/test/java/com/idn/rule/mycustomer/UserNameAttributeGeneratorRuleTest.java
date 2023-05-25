package com.idn.rule.mycustomer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.idn.rule.mycustomer.UserNameAttributeGeneratorRule;

import sailpoint.api.SailPointContext;
import sailpoint.object.Application;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.QueryOptions;

public class UserNameAttributeGeneratorRuleTest {
	@Mock
	SailPointContext context;
	
	@Rule 
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@Spy
	UserNameAttributeGeneratorRule userNameAttributeGeneratorRule = new UserNameAttributeGeneratorRule();
	
	Identity identity;
	Application application;
	
	String firstName;
	String lastName;
	
	@Before
	public void setup() {
		application = new Application();
	}
	
	private void init() throws Exception {
		identity = new Identity();
		identity.setFirstname(firstName);
		identity.setLastname(lastName);
		
		userNameAttributeGeneratorRule.identity = identity;
		userNameAttributeGeneratorRule.application = application;
		userNameAttributeGeneratorRule.context = context;
	}
	
	private void simulateCountLinkObjects(String displayName, int numberOfObjects) throws Exception {
		QueryOptions ops = new QueryOptions();
		ops.addFilter( Filter.ignoreCase( Filter.eq( "displayName", displayName ) ) );
		ops.addFilter( Filter.eq( "application", application ) );
		
		when (context.countObjects(Link.class, ops)).thenReturn(numberOfObjects);
	}
	
	@Test
	public void testCaseUsernameNotExist() throws Exception {
		this.firstName = "John";
		this.lastName = "Smith";
		
		init();
		simulateCountLinkObjects("John.Smith", 0);
		
		String username = userNameAttributeGeneratorRule.executeRule();
		
		assertEquals("John.Smith", username);
	}

	@Test
	public void testCase1UsernameExist() throws Exception {
		this.firstName = "John";
		this.lastName = "Smith";
		
		init();
		simulateCountLinkObjects("John.Smith", 1);
		simulateCountLinkObjects("John.Smith0", 0);
		
		String username = userNameAttributeGeneratorRule.executeRule();
		
		assertEquals("John.Smith0", username);
	}
	
	@Test
	public void testCase2UsernameExist() throws Exception {
		this.firstName = "John";
		this.lastName = "Smith";
		
		init();
		simulateCountLinkObjects("John.Smith", 1);
		simulateCountLinkObjects("John.Smith0", 1);
		simulateCountLinkObjects("John.Smith1", 1);
		simulateCountLinkObjects("John.Smith2", 1);
		simulateCountLinkObjects("John.Smith3", 1);
		simulateCountLinkObjects("John.Smith4", 0);
		
		String username = userNameAttributeGeneratorRule.executeRule();
		
		assertEquals("John.Smith4", username);
	}

}
