package fr.naoj.embeddable.archive;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Holds the content of a resource in an {@link Archive}.
 * 
 * @author Johann Bernez
 */
public class ResourceContent implements Content {

	private File resource;
	
	public ResourceContent(String resourceName) {
		PrivilegedAction<ClassLoader> action = new PrivilegedAction<ClassLoader>() {
	        public ClassLoader run() {
	            return Thread.currentThread().getContextClassLoader();
	        }
	    };
        
		URL resourceUrl = AccessController.doPrivileged(action).getResource(resourceName);
		String resourcePath = resourceUrl.getFile();
        try {
            // Have to URL decode the string as the ClassLoader.getResource(String) returns an URL encoded URL
            resourcePath = URLDecoder.decode(resourcePath, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalArgumentException(uee);
        }
		this.resource = new File(resourcePath);
	}

	public InputStream getInputStream() {
		try {
			return new BufferedInputStream(new FileInputStream(resource), 8192);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Can't open the resource" + resource.getName());
		}
	}
}
