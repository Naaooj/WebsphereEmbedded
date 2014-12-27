package fr.naoj.services;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.embeddable.EJBContainer;

import org.junit.Test;

import fr.naoj.entity.Person;

/**
 * <p>Test classe running the websphere embedded container.</p>
 * 
 * <p>In order to succeed, the datasource definition must match and the database must contains at
 * least one {@link Person} for the id with value 1.</p>
 * 
 * <p>More information about the configuration
 * through this <a href="http://www-01.ibm.com/support/knowledgecenter/SSAW57_8.5.5/com.ibm.websphere.nd.doc/ae/rejb_emconproperties.html?cp=SSAW57_8.5.5">link</a>
 * </p>
 * 
 * @author Johann Bernez
 */
public class EmbeddedContainerTest {

	@Test
	public void testJavaConfiguration() throws Exception {
		// Create the map that will contains the properties necessary to initialize the embedded container
		Map<String, Object> properties = new HashMap<String, Object>();
		
		// Set the datasource definition, the name must match the one that 
		// is defined in the persistence.xml file
		properties.put("DataSource.ds1.name", "jdbc/testDB");
		properties.put("DataSource.ds1.className", "com.ibm.db2.jcc.DB2DataSource");
		properties.put("DataSource.ds1.driverType", "4");
		properties.put("DataSource.ds1.databaseName", "TESTDB");
		properties.put("DataSource.ds1.serverName", "localhost");
		properties.put("DataSource.ds1.portNumber", "50000");
		properties.put("DataSource.ds1.user", "db2admin");
		properties.put("DataSource.ds1.password", "password");
		
		// Set the log folder for the embedded container transactions
		properties.put("com.ibm.websphere.tx.tranLogDirectory", "target/tranlog");
		
		// Define the binding name OF EACH enterprise bean that are defined. If this property miss
		// for only one bean, the container will not start at all
		properties.put("Bean.#classes#TestService.ResourceRef.BindingName.jdbc/testDB", "jdbc/testDB");

		// Try to create the container
		EJBContainer ec = EJBContainer.createEJBContainer(properties);
		assertNotNull("The container has not been created properly, probably a mistake in the properties", ec);
		
		// Lookup globally to find the desired service
		TestService service = (TestService) ec.getContext().lookup("java:global/classes/TestService");
		assertNotNull("The lookup succeed but returns a null value, check your configuration", service);
		
		// Search a person in the database
		Person person = service.findById(1);
		assertNotNull("Either you did not insert a person with id 1 or the service is not working", person);
	}
}
