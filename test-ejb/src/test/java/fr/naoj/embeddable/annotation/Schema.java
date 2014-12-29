package fr.naoj.embeddable.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define the name of the database schema associated with the datasource of the embedded container.
 * 
 * @author Johann Bernez
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Schema {
	String value() default "";
}
