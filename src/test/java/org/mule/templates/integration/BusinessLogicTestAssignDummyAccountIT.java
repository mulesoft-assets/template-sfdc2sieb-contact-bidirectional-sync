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
public class BusinessLogicTestAssignDummyAccountIT extends AbstractTemplateTestCase {

    private static final String ANYPOINT_TEMPLATE_NAME = "sfdc2sieb-contact-bidirectional-sync";
    private static final String SALESFORCE_INBOUND_FLOW_NAME = "triggerFlowToSiebel";
    private static final String SIEBEL_INBOUND_FLOW_NAME = "triggerFlowToSalesforce";
    private static final String ACCOUNT_ID_IN_SALESFORCE = "0012000001AOHJWAA5";
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

        System.setProperty("account.sync.policy", "assignDummyAccount");
        System.setProperty("account.id.in.b", ACCOUNT_ID_IN_SIEBEL);
        System.setProperty("account.id.in.a", ACCOUNT_ID_IN_SALESFORCE);
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
        System.clearProperty("watermark.default.expression");
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
        // Flow for creating contacts in sfdc A instance
        createContactInSalesforceFlow = getSubFlow("createContactInSalesforceFlow");
        createContactInSalesforceFlow.initialise();

        // Flow for creating contacts in sfdc B instance
        createContactInSiebelFlow = getSubFlow("createContactInSiebelFlow");
        createContactInSiebelFlow.initialise();

        // Flow for deleting contacts in sfdc A instance
        deleteContactFromSalesforceFlow = getSubFlow("deleteContactFromSalesforceFlow");
        deleteContactFromSalesforceFlow.initialise();

        // Flow for deleting contacts in sfdc B instance
        deleteContactFromSiebelFlow = getSubFlow("deleteContactFromSiebelFlow");
        deleteContactFromSiebelFlow.initialise();

        // Flow for querying the contact in sfdc A instance
        queryContactFromSalesforceFlow = getSubFlow("queryContactFromSalesforceFlow");
        queryContactFromSalesforceFlow.initialise();

        // Flow for querying the contact in sfdc B instance
        queryContactFromSiebelFlow = getSubFlow("queryContactFromSiebelFlow");
        queryContactFromSiebelFlow.initialise();

        // Flow for creating accounts in sfdc A instance
        createAccountInSalesforceFlow = getSubFlow("createAccountInSalesforceFlow");
        createAccountInSalesforceFlow.initialise();

        // Flow for creating accounts in sfdc B instance
        createAccountInSiebelFlow = getSubFlow("createAccountInSiebelFlow");
        createAccountInSiebelFlow.initialise();

        // Flow for querying accounts in sfdc A instance
        queryContactsAccountNameFromSalesforceFlow = getSubFlow("queryContactsAccountNameFromSalesforceFlow");
        queryContactsAccountNameFromSalesforceFlow.initialise();

        // Flow for querying accounts in sfdc B instance
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
    public void whenUpsartingAContactInInstanceBTheBelongingContactGetsUpsartedInInstanceA()
            throws MuleException, Exception {
        // Build test contacts
        String uniqueEmail = ANYPOINT_TEMPLATE_NAME + "-" + System.currentTimeMillis() + "@insalesforce.com";
        SfdcObjectBuilder contact = aContact()
                .with("FirstName", "Steve")
                .with("LastName", "Smith")
                .with("MailingCountry", "US")
                .with("Email", uniqueEmail);

        Map<String, Object> salesforceContact = contact.with("AccountId", ACCOUNT_ID_IN_SALESFORCE).build();
        Map<String, Object> salesforceContactEA = contact.with("Email Address", uniqueEmail).build();

        // Create contact in sand-box and keep track of it for posterior cleaning up
        contactsCreatedInSalesforce.add(createTestContactsInSfdcSandbox(salesforceContact, createContactInSalesforceFlow));

        // Execution
        executeWaitAndAssertBatchJob(SALESFORCE_INBOUND_FLOW_NAME);

        // Assertions
        Map<String, String> retrievedContactFromSalesforce = (Map<String, String>) querySalesforceContact(salesforceContactEA, queryContactFromSalesforceFlow);
        Map<String, String> retrievedContactFromSiebel = (Map<String, String>) querySiebelContact(salesforceContact, queryContactFromSiebelFlow);

        Assert.assertEquals("Email is not synchronized between systems.", salesforceContact.get("Email"), retrievedContactFromSiebel.get("Email Address"));
        Assert.assertEquals("FirstName is not synchronized between systems.", salesforceContact.get("FirstName"), retrievedContactFromSiebel.get("First Name"));
        Assert.assertEquals("LastName is not synchronized between systems.", salesforceContact.get("LastName"), retrievedContactFromSiebel.get("Last Name"));
        contactsCreatedInSiebel.add(retrievedContactFromSiebel.get("Id"));

        Map<String, HashMap> retrievedContactsAccountIdFromSalesforce = (Map<String, HashMap>) querySalesforceContact(salesforceContact, queryContactsAccountNameFromSalesforceFlow);
        Map<String, HashMap> retrievedContactsAccountIdFromSiebel = (Map<String, HashMap>) querySiebelContact(salesforceContact, queryContactsAccountNameFromSiebelFlow);

        String salesforceAccount = String.valueOf(retrievedContactsAccountIdFromSalesforce.get("Account").get("Name"));
        String siebelAccount = String.valueOf(retrievedContactsAccountIdFromSiebel.get("Account"));

        Assert.assertEquals("Account is not synchronized between systems.", salesforceAccount, siebelAccount);

    }

    private Object querySalesforceContact(Map<String, Object> contact, InterceptingChainLifecycleWrapper queryContactFlow)
            throws MuleException, Exception {

        Object payload = queryContactFlow.process(getTestEvent(contact, MessageExchangePattern.REQUEST_RESPONSE))
                .getMessage().getPayload();

        return payload;
    }

    private Object querySiebelContact(Map<String, Object> contact, InterceptingChainLifecycleWrapper queryContactFlow)
            throws MuleException, Exception {

        List<Map<String, Object>> payload = (List<Map<String, Object>>) queryContactFlow.process(getTestEvent(contact,
                MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();

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

    private void executeWaitAndAssertBatchJob(String flowConstructName)
            throws Exception {

        // Execute synchronization
        runSchedulersOnce(flowConstructName);

        // Wait for the batch job execution to finish
        batchTestHelper.awaitJobTermination(TIMEOUT_MILLIS * 1000, 500);
        batchTestHelper.assertJobWasSuccessful();
    }


}
