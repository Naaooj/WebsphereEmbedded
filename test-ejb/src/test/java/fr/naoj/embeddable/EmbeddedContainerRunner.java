package fr.naoj.embeddable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.embeddable.EJBContainer;
import javax.naming.NamingException;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * Runner that is responsible to initialize the embedded container. Then, it scans the test class
 * to inject any bean that must be used, managed by the embedded container.
 * 
 * @author Johann Bernez
 */
public class EmbeddedContainerRunner extends BlockJUnit4ClassRunner {
	
	private List<Field> fields = new ArrayList<Field>();
	private EJBContainer ec;
	
	public EmbeddedContainerRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		
		// Search in the test class all the field that must be injected
		Class<?> currentClass = clazz;
		while (currentClass != null) {
			for (Field f : currentClass.getDeclaredFields()) {
				f.setAccessible(true);
				if (f.isAnnotationPresent(EJB.class)) {
					fields.add(f);
				}
			}
			currentClass = currentClass.getSuperclass();
		}

		initializeContainer();
	}
	
	@Override
	public void run(RunNotifier notifier) {
		super.run(notifier);
		if (ec != null) {
			ec.close();
		}
	}
	
	@Override
	protected Object createTest() throws Exception {
		Object test = super.createTest();
		for (Field f : this.fields) {
			try {
				f.set(test, lookup(f.getType()));
			} catch (Exception e) {
				throw new InitializationError(Arrays.asList(new Throwable[]{e}));
			}
		}
		
		return test;
	}
	
	protected void initializeContainer() throws InitializationError {
		try {
			// Map that will contains all the properties necessary 
			// for the embedded container
			Map<String, Object> properties = new HashMap<String, Object>();
						
			properties.put("com.ibm.websphere.embeddable.configFileName", "src/test/resources/embeddable.properties");
			
			ec = EJBContainer.createEJBContainer(properties);
		} catch (Exception e) {
			throw new InitializationError(Arrays.asList(new Throwable[]{e}));
		}
	}
	
	/**
	 * <p>Dookup for a given bean. When the lookup has to made
	 * manually, here is the type of jdni to use :<br/>
	 * <code>lookup("java:global/MODULE_NAME/BEAN_NAME");</code><br/>
	 * for exemple :<br/>
	 * <code>lookup("java:global/classes/TestService");</code></p>
	 * 
	 * @param ejbClass
	 * @return
	 * @throws NamingException
	 */
	@SuppressWarnings("unchecked")
	private <T> T lookup(Class<T> ejbClass) throws NamingException {
		return (T) ec.getContext().lookup("java:global/classes/"+ejbClass.getSimpleName());
	}
}
