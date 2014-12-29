package fr.naoj.embeddable.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define the dataset to use for a unit test or more precisely on each methods.
 * 
 * @author Johann Bernez
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSetDefinition {
	/**
	 * @return the name of the resource to load
	 */
	String value();
	
	/**
	 * @return <code>true</code> a delete operation must be performed 
	 * for the dataset when the annotated method is reached
	 */
	boolean delete() default true;
}
