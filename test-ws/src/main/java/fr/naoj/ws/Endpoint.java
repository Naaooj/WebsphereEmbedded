package fr.naoj.ws;

import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import fr.naoj.entity.Person;
import fr.naoj.services.TestService;

@WebService(name="test", serviceName="testService")
public class Endpoint {
	
	@EJB
	private TestService service;
	
	@WebMethod()
	@WebResult(name="person")
	public Person findById(@WebParam(name="id") Integer id) {
		return service.findById(id);
	}
	
	@WebMethod()
	@WebResult(name="person")
	public Person createNew(@WebParam(name="firstname") String firstname, @WebParam(name="lastname") String lastname) {
		return service.createNew(firstname, lastname);
	}
}
