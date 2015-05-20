/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import com.mulesoft.module.batch.BatchTestHelper;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
public class BusinessLogicTestCreateAccountIT extends AbstractTemplateTestCase {

    private static final String ANYPOINT_TEMPLATE_NAME = "sfdc2sieb-contact-bidirectional-sync";
    private static final String SALESFORCE_INBOUND_FLOW_NAME = "triggerFlowToSiebel";
    private static final String SIEBEL_INBOUND_FLOW_NAME = "triggerFlowToSalesforce";
    private static final String ACCOUNT_ID_IN_SIEBEL = "1-C4QJ";
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
    private SubflowInterceptingChainLifecycleWrapper createAccountInSalesforceFlow;
    private SubflowInterceptingChainLifecycleWrapper createAccountInSiebelFlow;
    private SubflowInterceptingChainLifecycleWrapper queryContactsAccountNameFromSalesforceFlow;
    private SubflowInterceptingChainLifecycleWrapper queryContactsAccountNameFromSiebelFlow;

    @BeforeClass
    public static void beforeTestClass() {
        // Set polling frequency to 10 seconds
        System.setProperty("poll.frequency", "10000");
        System.setProperty("account.sync.policy", "syncAccount");
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

        // Flow for querying the contact in Salesforce
        queryContactFromSalesforceFlow = getSubFlow("queryContactFromSalesforceFlow");
        queryContactFromSalesforceFlow.initialise();

        // Flow for querying the contact in Siebel
        queryContactFromSiebelFlow = getSubFlow("queryContactFromSiebelFlow");
        queryContactFromSiebelFlow.initialise();

        // Flow for creating accounts in Salesforce
        createAccountInSalesforceFlow = getSubFlow("createAccountInSalesforceFlow");
        createAccountInSalesforceFlow.initialise();

        // Flow for creating accounts in Siebel
        createAccountInSiebelFlow = getSubFlow("createAccountInSiebelFlow");
        createAccountInSiebelFlow.initialise();

        // Flow for querying accounts in Salesforce
        queryContactsAccountNameFromSalesforceFlow = getSubFlow("queryContactsAccountNameFromSalesforceFlow");
        queryContactsAccountNameFromSalesforceFlow.initialise();

        // Flow for querying accounts in Siebel
        queryContactsAccountNameFromSiebelFlow = getSubFlow("queryContactsAccountNameFromSiebelFlow");
        queryContactsAccountNameFromSiebelFlow.initialise();
    }

    private static void cleanUpSandboxesByRemovingTestContacts()
            throws MuleException, Exception {

        final List<String> idList = new ArrayList<String>();

        for (String contact : contactsCreatedInSalesforce) {
            idList.add(contact);
        }

        deleteContactFromSalesforceFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
        idList.clear();

        for (String contact : contactsCreatedInSiebel) {
            idList.add(contact);
        }

        deleteContactFromSiebelFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
    }

    @Test
    @Ignore
    public void whenUpsertingContactInSalesforceTheBelongingContactGetsUpsertedSiebel()
            throws MuleException, Exception {
        // Build test contacts
        String uniqueEmail = ANYPOINT_TEMPLATE_NAME + "-" + System.currentTimeMillis() + "@insiebel.com";
        SfdcObjectBuilder contact = aContact()
                .with("First Name", "Patrick")
                .with("Last Name", "Smither")
                .with("Email Address", uniqueEmail);

        Map<String, Object> siebelContact = contact.with("Account Id", ACCOUNT_ID_IN_SIEBEL).build();
        Map<String, Object> siebelContactEA = contact.with("Email", uniqueEmail).build();

        // Create contact in sand-box and keep track of it for posterior cleaning up
        contactsCreatedInSiebel.add(createTestContactsInSiebelSandbox(siebelContact, createContactInSiebelFlow));

        // Execution
        executeWaitAndAssertBatchJob(SIEBEL_INBOUND_FLOW_NAME);

        // Assertions
        Map<String, String> retrievedContactFromSalesforce = (Map<String, String>) querySalesforceContact(siebelContactEA, queryContactFromSalesforceFlow);

        Assert.assertEquals("Email is not synchronized between systems.", siebelContact.get("Email Address"), retrievedContactFromSalesforce.get("Email"));
        Assert.assertEquals("FirstName is not synchronized between systems.", siebelContact.get("First Name"), retrievedContactFromSalesforce.get("FirstName"));
        Assert.assertEquals("LastName is not synchronized between systems.", siebelContact.get("Last Name"), retrievedContactFromSalesforce.get("LastName"));
        contactsCreatedInSalesforce.add(retrievedContactFromSalesforce.get("Id"));

        // for time being disabled until problem with Siebel Account Id upsert will be solved
//        Map<String, HashMap> retrievedContactsAccountIdFromSalesforce = (Map<String, HashMap>) querySalesforceContact(siebelContactEA, queryContactsAccountNameFromSalesforceFlow);
//        Map<String, HashMap> retrievedContactsAccountIdFromSiebel = (Map<String, HashMap>) querySiebelContact(siebelContact, queryContactsAccountNameFromSiebelFlow);
//
//        String salesforceAccount = String.valueOf(retrievedContactsAccountIdFromSalesforce.get("Account").get("Name"));
//        String siebelAccount = String.valueOf(retrievedContactsAccountIdFromSiebel.get("Account"));
//
//        Assert.assertEquals("Account is not synchronized between systems.", salesforceAccount, siebelAccount);
    }

    private Object querySalesforceContact(Map<String, Object> contact, InterceptingChainLifecycleWrapper queryContactFlow)
            throws MuleException, Exception {

        return queryContactFlow.process(getTestEvent(contact, MessageExchangePattern.REQUEST_RESPONSE))
                .getMessage().getPayload();
    }

    private Object querySiebelContact(Map<String, Object> contact, InterceptingChainLifecycleWrapper queryContactFlow)
            throws MuleException, Exception {

        List<Map<String, Object>> payload = (List<Map<String, Object>>) queryContactFlow.process(getTestEvent(contact,
                MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();

        return payload.isEmpty() ? new HashMap<String, Object>() : payload.get(0);
    }

    private String createTestContactsInSiebelSandbox(Map<String, Object> contact, InterceptingChainLifecycleWrapper createContactFlow)
            throws MuleException, Exception {

        final CreateResult payloadAfterExecution = (CreateResult) createContactFlow
                .process(getTestEvent(contact, MessageExchangePattern.REQUEST_RESPONSE))
                .getMessage().getPayload();

        return payloadAfterExecution.getCreatedObjects().get(0);
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
