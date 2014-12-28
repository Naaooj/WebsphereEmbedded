WebsphereEmbedded
=================

Projects aimed at testing websphere 8.5 embedded container.   

This project contains tags corresponding to the various ways of using the embedded container, from
the basic to the more complicated.

If you have any trouble, do not hesitate to send me an email.

## Requirements
There is only one test written, inside the test-ejb project. If you want the test to pass, your 
database must at least contains one entry for the table PERSON with the id=1.   
In order to test the embedded container, [Websphere for Developer](http://www.ibm.com/developerworks/downloads/ws/wasdevelopers/) 
must be installed. A database server of your choice must also be present, here [DB2 Express-C](http://www-01.ibm.com/software/data/db2/express-c/download.html) is used.
After websphere is installed, the installation path must be configured in test-ejb/pom.xml.   

## How it works
Almost everything is managed by the `EmbeddableContainerRunner`. The runner is responsible to
initialize the embedded container and to inject the EJB in the test class. By creating an `Archive`
through an annotated method in your test classes, only the things that matter for your test can be
added to the archive. 
Although the behavior becomes easier, we can't still use custom data with our tests. One way to achieve this
is by using DBUnit, look at the next tag : enhance-with-dbunit

## Things to remember   
Wesphere uses Openjpa, which needs to enhance entites at compile time or during runtime.   
At runtime, the Openjpa java agent must be used when initializing the VM, or some openjpa settings
must be added the the persistence.xml. Using the the java agent with eclipse is really easy, you just have
to pass it to the VM like this : -javaagent:"C:\Users\Naoj\.m2\repository\org\apache\openjpa\openjpa\2.1.1\openjpa-2.1.1.jar".
But when you are using maven, using the agent with surefire is really, really hard! The simplest to enhance
entities is through the configuration. 

The way to avoid using the agent is to tune openjpa. First you need to define all the entities 
inside the <class> tag in your persistence.xml test file.   
And then you simply set these two openjpa settings :
* openjpa.jdbc.SynchronizeMappings : tels openjpa to add the entities in the context for enhancement
* openjpa.RuntimeUnenhancedClasses : activates runtime enhencement for the defined entities

### Enable tracing
If you want to activate the websphere embedded traces, simply add this to the VM arguments : -Dcom.ibm.ejs.ras.lite.traceSpecification=EJBContainer=all:MetaData=all

### Common pitfalls
Here is a list of all the exceptions that you may encounter during your test processes.

#### When you forget to use : driverType=4
`CWWJP0013E: The server cannot locate the java:comp/env/jdbc/testDB data source for the test-jpa persistence unit because it has encountered the following exception: com.ibm.websphere.naming.CannotInstantiateObjectException: Exception occurred while the JNDI NamingManager was processing a javax.naming.Reference object. [Root exception is com.ibm.websphere.naming.CannotInstantiateObjectException: Exception occurred while the JNDI NamingManager was processing a javax.naming.Reference object. [Root exception is javax.resource.ResourceException: Required driverType property was not specifed or is invalid. The driverType property is null]].`

#### When you do not include your jdbc driver in your classpath (as a maven dependency)
`CNTR0020E: EJB threw an unexpected (non-declared) exception during invocation of method "findById" on bean "BeanId(embeddable#classes#TestService, null)". Exception data: <openjpa-2.2.3-SNAPSHOT-r422266:1564471 fatal user error> org.apache.openjpa.persistence.ArgumentException: The persistence provider is attempting to use properties in the persistence.xml file to resolve the data source. A Java Database Connectivity (JDBC) driver or data source class name must be specified in the openjpa.ConnectionDriverName or javax.persistence.jdbc.driver property. The following properties are available in the configuration: "WsJpaJDBCConfigurationImpl@3ac78bcc: PDQ disabled: AccessIntent Task=disable".`

#### When the persistence.xml file can not be found in the classpath
`CWNEN0035E: The fr.naoj.service.TestService/manager reference of type javax.persistence.EntityManager for the TestService component in the classes module of the embeddable application cannot be resolved. CNTR0019E: EJB threw an unexpected (non-declared) exception during invocation of method "findById". Exception data: javax.ejb.EJBException: The fr.naoj.service.TestService/manager reference of type javax.persistence.EntityManager for the TestService component in the classes module of the embeddable application cannot be resolved.`

#### When you do not map the datasource to an ejb (Bean.#classes#YourBeanName.ResourceRef.BindingName)
`CWNEN0044E: A resource reference binding could not be found for the jdbc/testDB resource reference, defined for the TestService component.CWNEN0011E: The injection engine failed to process bindings for the metadata due to the following error: CWNEN0044E: A resource reference binding could not be found for the following resource references [jdbc/testDB], defined for the TestService component.WSVR0040E: addEjbModule failed for test-ejb-0.0.1-SNAPSHOT.jar com.ibm.wsspi.injectionengine.InjectionException: CWNEN0044E: A resource reference binding could not be found for the following resource references [jdbc/testDB], defined for the TestService component.`

#### When you make a typo mistake in the container settings (DadaSource.ds1..., instead DataSource.ds1...) or when tha database is not started
`CNTR0020E: EJB threw an unexpected (non-declared) exception during invocation of method "findById" on bean "BeanId(embeddable#test-ejb-0.0.1-SNAPSHOT.jar#TestService, null)". Exception data: <openjpa-2.2.3-SNAPSHOT-r422266:1564471 nonfatal general error> org.apache.openjpa.persistence.PersistenceException: There were errors initializing your configuration: <openjpa-2.2.3-SNAPSHOT-r422266:1564471 fatal user error> org.apache.openjpa.util.UserException: A connection could not be obtained for driver class "null" and URL "null".  You may have specified an invalid URL.`
