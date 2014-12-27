package fr.naoj.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name="PERSON", schema="test")
@NamedQueries({
	@NamedQuery(name="Person.selectAll", query="select p from Person p")
})
@XmlRootElement(name="person")
public class Person implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(generator="person_seq_generator", strategy=GenerationType.SEQUENCE)
	@SequenceGenerator(name="person_seq_generator", sequenceName="test.person_seq")
	private Integer id;
	
	@Column
	private String lastname;
	
	@Column
	private String firstname;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getLastname() {
		return lastname;
	}
	
	public void setLastname(String lastname) {
		this.lastname = lastname;
	}
	
	public String getFirstname() {
		return firstname;
	}
	
	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

}
