package fr.naoj.services;

import static org.junit.Assert.*;

import javax.ejb.EJB;

import org.junit.Test;
import org.junit.runner.RunWith;

import fr.naoj.embeddable.EmbeddedContainerRunner;
import fr.naoj.embeddable.annotation.DataSetDefinition;
import fr.naoj.embeddable.annotation.Module;
import fr.naoj.embeddable.annotation.Schema;
import fr.naoj.embeddable.archive.Archive;
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
@Schema(value="test")
@RunWith(EmbeddedContainerRunner.class)
public class EmbeddedContainerTest {
	
	@EJB
	private TestService service;

	@Test
	@DataSetDefinition(value="PersonDataSet.xml", delete=true)
	public void testJunitRunner() throws Exception {
		// The runner has normally injected the EJB
		assertNotNull("The lookup succeed but returns a null value, check your configuration", service);
		
		// Search a person in the temporary database data
		Person person = service.findById(10);
		assertNotNull("Either you did not insert a person with id 1 or the service is not working", person);
		
		// Checks the name
		assertEquals("John", person.getFirstname());
	}
	
	@Module(bindingName="jdbc/testDB")
	public static Archive createModule() {
		return Archive.create("test.war")
					  .addClass(TestService.class)
					  .addResource("test-persistence.xml", "META-INF/persistence.xml");
	}
}
