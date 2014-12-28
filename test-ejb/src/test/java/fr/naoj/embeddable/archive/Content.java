package fr.naoj.embeddable.archive;

import java.io.InputStream;

/**
 * Any type of content must be retrieved through an {@link InputStream}.
 * 
 * @author Johann Bernez
 */
public interface Content {
	
	InputStream getInputStream();
	
	static final Helper HELPER = new Helper();
	
	public final class Helper {
		public String getJavaClassPath(Class<?> clazz) {
			return clazz.getName().replaceAll("\\.", "/") + ".class";
		}
	}
}
