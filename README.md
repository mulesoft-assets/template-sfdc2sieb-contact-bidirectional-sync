
# Anypoint Template: Salesforce to Siebel Contact Bidirectional Sync

# License Agreement
This template is subject to the conditions of the 
<a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>.
Review the terms of the license before downloading and using this template. You can use this template for free 
with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case
As a Salesforce administrator I want to have my Contacts synchronized between Salesforce and Oracle Siebel Business Objects.

This template serves as a foundation for setting an online bidirectional sync of contacts between Salesforce and Oracle Siebel Business Objects, being able to specify filtering criteria. 

The integration is triggered by a scheduler defined in the flow. It is getting changes (new Contacts or modified ones) that have occurred either in Salesforces or Siebel during a certain defined period of time. For those Contacts that both have not been updated yet the integration triggers an upsert (update or create depending the case) taking the last modification as the one that should be applied.

The integration also migrates associated accounts to the destination system as well. Account synchronization policy must be `syncAccount`.

Requirements have been set not only to be used as examples, but also to establish starting points 
to adapt the integration to any given requirements.

# Considerations

To make this Anypoint Template run, there are certain preconditions that must be considered. All of them deal with the preparations in both, that must be made in order for all to run smoothly. **Failing to do so could lead to unexpected behavior of the template.**



## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work.

### FAQ

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>
- Can I modify the Field Access Settings? How? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US">Salesforce: Modifying Field Access Settings</a>

### As a Data Source

If the user who configured the template for the source system does not have at least *read only* permissions for the fields that are fetched, then an *InvalidFieldFault* API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault [ApiQueryFault [ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='
Account.Phone, Account.Rating, Account.RecordTypeId, Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attempting to use a custom field, be sure to append the '__c' after the custom field name. Reference your WSDL or the describe call for the appropriate names.'
]
row='1'
column='486'
]
]
```

### As a Data Destination

There are no considerations with using Salesforce as a data destination.
## Siebel Considerations

Here's what you need to know to get this template to work with Siebel.

This template may use date time or timestamp fields from Siebel to do comparisons and take further actions.
While the template handles the time zone by sending all such fields in a neutral time zone, it cannot discover the time zone in which the Siebel instance is on.
It is up to you to provide such information. See [Oracle's Setting Time Zone Preferences](http://docs.oracle.com/cd/B40099_02/books/Fundamentals/Fund_settingoptions3.html)


### As a Data Source

There are no considerations with using Siebel as a data origin.
### As a Data Destination

In order to make the Siebel connector work smoothly you have to provide the correct version of the Siebel jars that works with your Siebel installation. [See more](https://docs.mulesoft.com/connectors/siebel-connector#prerequisites).








# Run it!
Simple steps to get Salesforce to Siebel Contact Bidirectional Sync running.


## Running On Premises
In this section we help you run your template on your computer.


### Where to Download Anypoint Studio and the Mule Runtime
If you are a newcomer to Mule, here is where to get the tools.

+ [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
+ [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)


### Importing a Template into Studio
In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your
Anypoint Platform credentials, search for the template, and click **Open**.


### Running on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources.
+ Complete all the properties required as per the examples in the "Properties to Configure" section.
+ Right click the template project folder.
+ Hover your mouse over `Run as`
+ Click `Mule Application (configure)`
+ Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`
+ Click `Run`


### Running on Mule Standalone
Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable. To follow the example, this is `mule.env=prod`. 


## Running on CloudHub
While creating your application on CloudHub (or you can do it later as a next step), go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the **mule.env**.


### Deploying your Anypoint Template on CloudHub
Studio provides an easy way to deploy your template directly to CloudHub, for the specific steps to do so check this


## Properties to Configure
To use this template, configure properties (credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.
### Application Configuration
+ page.size `200`            
+ scheduler.frequency `60000`
+ scheduler.start.delay `1000`
+ watermark.default.expression.sfdc `2018-10-01T03:00:59Z`
+ watermark.default.expression.sieb `2018-10-01T03:00:59Z`
+ account.sync.policy `syncAccount`

**Salesforce Connector configuration**
+ sfdc.username `bob.dylan@org`
+ sfdc.password `DylanPassword123`
+ sfdc.securityToken `avsfwCUl7apQs56Xq2AKi3X`

**Oracle Siebel Business Objects Connector configuration**
+ sieb.user `SADMIN`
+ sieb.password `SADMIN`
+ sieb.server `your_server`
+ sieb.serverName `your_server_name`
+ sieb.objectManager `your_object_manager`
+ sieb.port `your_port`

# API Calls
Salesforce imposes limits on the number of API Calls that can be made. Therefore calculating this amount may be an important factor to consider. The Anypoint Template calls to the API can be calculated using the formula:

***1 + X + X / ${page.size}***

Being ***X*** the number of Contacts to be synchronized on each run. 

The division by ***${page.size}*** is because, by default, Contacts are gathered in groups of ${page.size} for each Upsert API Call in the aggregation step. Also consider that these calls are executed repeatedly every polling cycle.	

For instance if 10 records are fetched from origin instance, then 12 api calls will be made (1 + 10 + 1).


# Customize It!
This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.
As Mule applications are based on XML files, this page describes the XML files used with this template.

More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

* config.xml
* businessLogic.xml
* endpoints.xml
* errorHandling.xml


## config.xml
Configuration for connectors and configuration properties are set in this file. Even change the configuration here, all parameters that can be modified are in properties file, which is the recommended place to make your changes. However if you want to do core changes to the logic, you need to modify this file.

In the Studio visual editor, the properties are on the *Global Element* tab.


## businessLogic.xml
This file holds the functional aspect of the template. Its main component is a Mule batch job, and 
it includes steps for both executing the synchronization from Salesforce to Oracle Siebel Business Objects, and the other way around.



## endpoints.xml
This file is conformed by a Flow containing the Scheduler that will periodically query either in Salesforce or Siebel for updated/created Contacts that meet the defined criteria in the query. And then executing the batch job process with the query results.



## errorHandling.xml
This is the right place to handle how your integration reacts depending on the different exceptions. 
This file provides error handling that is referenced by the main flow in the business logic.




