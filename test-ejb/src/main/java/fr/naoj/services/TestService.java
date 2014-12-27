package fr.naoj.services;

import java.util.List;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import fr.naoj.entity.Person;

@Stateless
@LocalBean
@Resource(name="jdbc/testDB", type=javax.sql.DataSource.class, authenticationType = AuthenticationType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class TestService {
	
	@PersistenceContext(type=PersistenceContextType.TRANSACTION)
	private EntityManager manager;

	public List<Person> query() {
		return this.manager.createNamedQuery("Person.selectAll", Person.class).getResultList();
	}

	public Person createNew(String firstname, String lastname) {
		Person p = new Person();
		p.setFirstname(firstname);
		p.setLastname(lastname);
		this.manager.persist(p);
		return p;
	}

	public Person findById(Integer id) {
		return this.manager.find(Person.class, id);
	}
}
