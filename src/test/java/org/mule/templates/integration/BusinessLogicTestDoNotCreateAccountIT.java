/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import com.mulesoft.module.batch.BatchTestHelper;
import com.sforce.soap.partner.SaveResult;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.modules.siebel.api.model.response.CreateResult;
import org.mule.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.builders.SfdcObjectBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mule.templates.builders.SfdcObjectBuilder.aContact;

/**
 * The objective of this class is validating the correct behavior of the flows
 * for this Mule Anypoint Template
 */
@SuppressWarnings("unchecked")
public class BusinessLogicTestDoNotCreateAccountIT extends AbstractTemplateTestCase {

    private static final String SALESFORCE_INBOUND_FLOW_NAME = "triggerFlowToSiebel";
    private static final String SIEBEL_INBOUND_FLOW_NAME = "triggerFlowToSalesforce";
    private static final int TIMEOUT_MILLIS = 60;

    private static List<String> contactsCreatedInSalesforce = new ArrayList<String>();
    private static List<String> contactsCreatedInSiebel = new ArrayList<String>();
    private static SubflowInterceptingChainLifecycleWrapper deleteContactFromSalesforceFlow;
    private static SubflowInterceptingChainLifecycleWrapper deleteContactFromSiebelFlow;

    private SubflowInterceptingChainLifecycleWrapper createContactInSalesforceFlow;
    private SubflowInterceptingChainLifecycleWrapper createContactInSiebelFlow;
    private InterceptingChainLifecycleWrapper queryContactFromSalesforceFlow;
    private InterceptingChainLifecycleWrapper queryContactFromSiebelFlow;
    private BatchTestHelper batchTestHelper;

    @BeforeClass
    public static void beforeTestClass() {
        System.setProperty("account.sync.policy", "");
        System.setProperty("poll.frequency", "10000");
    }

    @Before
    public void setUp() throws MuleException {
        stopAutomaticPollTriggering();
        getAndInitializeFlows();

        batchTestHelper = new BatchTestHelper(muleContext);
    }

    @AfterClass
    public static void shutDown() {
        System.clearProperty("poll.frequency");
        System.clearProperty("account.sync.policy");
    }

    @After
    public void tearDown() throws MuleException, Exception {
        cleanUpSandboxesByRemovingTestContacts();
    }

    private void stopAutomaticPollTriggering() throws MuleException {
        stopFlowSchedulers(SALESFORCE_INBOUND_FLOW_NAME);
        stopFlowSchedulers(SIEBEL_INBOUND_FLOW_NAME);
    }

    private void getAndInitializeFlows() throws InitialisationException {
        // Flow for creating contacts in Salesforce
        createContactInSalesforceFlow = getSubFlow("createContactInSalesforceFlow");
        createContactInSalesforceFlow.initialise();

        // Flow for creating contacts in Siebel
        createContactInSiebelFlow = getSubFlow("createContactInSiebelFlow");
        createContactInSiebelFlow.initialise();

        // Flow for deleting contacts in Salesforce
        deleteContactFromSalesforceFlow = getSubFlow("deleteContactFromSalesforceFlow");
        deleteContactFromSalesforceFlow.initialise();

        // Flow for deleting contacts in Siebel
        deleteContactFromSiebelFlow = getSubFlow("deleteContactFromSiebelFlow");
        deleteContactFromSiebelFlow.initialise();

        // Flow for querying contacts in Salesforce
        queryContactFromSalesforceFlow = getSubFlow("queryContactFromSalesforceFlow");
        queryContactFromSalesforceFlow.initialise();

        // Flow for querying contacts in Siebel
        queryContactFromSiebelFlow = getSubFlow("queryContactFromSiebelFlow");
        queryContactFromSiebelFlow.initialise();
    }

    private void cleanUpSandboxesByRemovingTestContacts()
            throws MuleException, Exception {

        final List<String> idList = new ArrayList<String>();

        for (String contact : contactsCreatedInSalesforce) {
            idList.add(contact);
        }

        if (!contactsCreatedInSalesforce.isEmpty()){
            deleteContactFromSalesforceFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
            idList.clear();
            contactsCreatedInSalesforce.clear();
        }

        for (String contact : contactsCreatedInSiebel) {
            idList.add(contact);
        }

        if(!contactsCreatedInSiebel.isEmpty()){
            deleteContactFromSiebelFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
            contactsCreatedInSiebel.clear();
        }
    }

    @Test
    public void whenUpdatingContactInSiebelTheBelongingContactGetsUpdatedInSalesforce()
            throws MuleException, Exception {

        Map<String, Object> siebelContact = createSiebelContact(1);

        // Create contacts in sand-boxes and keep track of them for posterior cleaning up
        contactsCreatedInSiebel.add(createTestContactsInSiebelSandbox(siebelContact, createContactInSiebelFlow));

        // Execution
        executeWaitAndAssertBatchJob(SIEBEL_INBOUND_FLOW_NAME);

        // Assertions
        Map<String, String> retrievedContactFromSalesforce = (Map<String, String>) querySalesforceContact(siebelContact, queryContactFromSalesforceFlow);
        Assert.assertEquals("Some contacts are not synchronized between systems.", siebelContact.get("Email Address"), retrievedContactFromSalesforce.get("Email"));
        // for cleaning up
        contactsCreatedInSalesforce.add(retrievedContactFromSalesforce.get("Id"));
}

    @Test
    public void whenUpdatingContactInSalesforceTheBelongingContactGetsUpdatedInSiebel()
            throws MuleException, Exception {
        // Build test contacts
        SfdcObjectBuilder contact = aContact()
                .with("FirstName", "Peter")
                .with("LastName", "Hobbit")
                .with("MailingCountry", "US")
                .with("Email", "insalesforce" + "-" + System.currentTimeMillis() + "@mail.com");

        Map<String, Object> salesforceContact = contact.build();

        // Create contacts in sand-boxes and keep track of them for posterior cleaning up
        contactsCreatedInSalesforce.add(createTestContactsInSfdcSandbox(salesforceContact, createContactInSalesforceFlow));

        // Execution
        logger.info("TEST--------------------------before main flow execution");
        executeWaitAndAssertBatchJob(SALESFORCE_INBOUND_FLOW_NAME);
        logger.info("TEST--------------------------after main flow execution");

        Map<String, String> retrievedContactFromSiebel = (Map<String, String>) querySiebelContact(salesforceContact, queryContactFromSiebelFlow);
        Assert.assertEquals("Some contacts are not synchronized between systems.", salesforceContact.get("Email"), retrievedContactFromSiebel.get("Email Address"));
        contactsCreatedInSiebel.add(retrievedContactFromSiebel.get("Id"));
    }

    private Object querySalesforceContact(Map<String, Object> contact, InterceptingChainLifecycleWrapper queryContactFlow)
            throws MuleException, Exception {

        return queryContactFlow.process(getTestEvent(contact, MessageExchangePattern.REQUEST_RESPONSE))
                .getMessage().getPayload();
    }

    private Object querySiebelContact(Map<String, Object> contact, InterceptingChainLifecycleWrapper queryContactFlow)
            throws MuleException, Exception {

    	List<Map<String, Object>> payload = (List<Map<String, Object>>) queryContactFlow.process(getTestEvent(contact, MessageExchangePattern.REQUEST_RESPONSE))
				        .getMessage().getPayload();
		return payload.isEmpty() ? new HashMap<String, Object>() : payload.get(0);
    }

    private String createTestContactsInSfdcSandbox(Map<String, Object> contact, InterceptingChainLifecycleWrapper createContactFlow)
            throws MuleException, Exception {

        List<Map<String, Object>> salesforceContacts = new ArrayList<Map<String, Object>>();
        salesforceContacts.add(contact);

        final List<SaveResult> payloadAfterExecution = (List<SaveResult>) createContactFlow
                .process(getTestEvent(salesforceContacts, MessageExchangePattern.REQUEST_RESPONSE))
                .getMessage().getPayload();

        return payloadAfterExecution.get(0).getId();
    }

    private String createTestContactsInSiebelSandbox(Map<String, Object> contact, InterceptingChainLifecycleWrapper createContactFlow)
            throws MuleException, Exception {

        final CreateResult payloadAfterExecution = (CreateResult) createContactFlow
                .process(getTestEvent(contact, MessageExchangePattern.REQUEST_RESPONSE))
                .getMessage().getPayload();

        return payloadAfterExecution.getCreatedObjects().get(0);
    }

	protected Map<String, Object> createSiebelContact(int sequence) {
		return SfdcObjectBuilder
				.aContact()
				.with("First Name", "FirstName_" + sequence)
				.with("Last Name", TEMPLATE_NAME + sequence)
				.with("Email Address", buildUniqueEmail("insiebel." + System.currentTimeMillis())).build();
	}

	protected String buildUniqueEmail(String contact) {
		String server = "fakemail";

		StringBuilder builder = new StringBuilder();
		builder.append( contact);
		builder.append("@");
		builder.append(server);
		builder.append(".com");

		return builder.toString();
	}

	private void executeWaitAndAssertBatchJob(String flowConstructName)
            throws Exception {

        // Execute synchronization
        runSchedulersOnce(flowConstructName);

        // Wait for the batch job execution to finish
        batchTestHelper.awaitJobTermination(TIMEOUT_MILLIS * 1000, 500);
        batchTestHelper.assertJobWasSuccessful();
    }


}
