package fr.naoj.embeddable.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fr.naoj.embeddable.archive.Archive;

/**
 * Indicates that a static method is responsible to create an {@link Archive}.
 * 
 * @author Johann Bernez
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Module {

	/**
	 * @return the name of the binding of a datasource
	 */
	String bindingName() default "";
}
