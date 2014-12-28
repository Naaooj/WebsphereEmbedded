package fr.naoj.embeddable.archive;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Holds the content of a {@link Class} in an {@link Archive}.
 * 
 * @author Johann Bernez
 */
public class ClassContent implements Content {

	private Class<?> clazz;
	
	public ClassContent(Class<?> clazz) {
		this.clazz = clazz;
	}

	public InputStream getInputStream() {
		ClassLoader classLoader = this.clazz.getClassLoader();
		if (classLoader == null) {
			classLoader = ClassLoader.getSystemClassLoader();
		}
		String resourceName = HELPER.getJavaClassPath(this.clazz);
		return new BufferedInputStream(classLoader.getResourceAsStream(resourceName), 8192);
	}
}
