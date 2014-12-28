package fr.naoj.embeddable.archive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class aimed at creating in the temporary folder an archive of a desired type that is going
 * to hold classes or preferences files.
 * 
 * @author Johann Bernez
 */
public class Archive {

	private String name;
	private static final Path ROOT = new Path("/");
	private Map<Path, Entry> entries = new HashMap<Path, Entry>();
	private File destFile;
	
	private Archive(String name) {
		this.name = name;
		addEntry(new Entry(ROOT));
	}
	
	public static Archive create(String name) {
		return new Archive(name);
	}
	
	public Archive addClass(Class<?>...classes) {
		for (Class<?> clazz : classes) {
			addEntry(new Entry(new ClassContent(clazz), new Path("/classes/" + Content.HELPER.getJavaClassPath(clazz))));
		}
		return this;
	}
	
	public Archive addResource(String resourceName, String asResource) {
		try {
			addEntry(new Entry(new ResourceContent(resourceName), new Path(asResource)));
		} catch (Exception e) {
			throw new RuntimeException("Resource " + resourceName + " not found", e);
		}
		return this;
	}
	
	/**
	 * @return the {@link File} that correspond to the archive. The archive is only created one time.
	 */
	public File asFile() {
		if (destFile == null) {
			write();
		}
		return destFile;
	}
	
	private void write() {
		destFile = new File(System.getProperty("java.io.tmpdir")+File.separator+this.name);
		try {
			if (destFile.exists()) {
				destFile.delete();
			}
			byte[] buf = new byte[2048];
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destFile));
			for (Entry node : this.entries.values()) {
				if (ROOT.equals(node.getPath())) {
					continue;
				}
				String path = node.getPath().get();
				if (path.startsWith("/")) {
					path = path.substring(1);
				}
				if (node.getContent() == null) {
					if (!path.endsWith("/")) {
						path += "/";
					}
					out.putNextEntry(new ZipEntry(path));
					out.closeEntry();
				} else {
					out.putNextEntry(new ZipEntry(path));
					InputStream in = node.asStream();
					int len;
		            while ((len = in.read(buf)) > 0) {
		                out.write(buf, 0, len);
		            }
		            in.close();
					out.closeEntry();
				}
			}
			out.close();
		} catch (Exception e) {
			destFile = null;
			throw new RuntimeException("Unable to create the archive", e);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, this.entries.get(ROOT));
		return sb.toString();
	}
	
	private void toString(StringBuilder sb, Entry node) {
		if (sb.length() > 0) {
			sb.append("\r\n");
		}
		sb.append(node.getPath().toString());
		for (Entry child : node.getChildren()) {
			toString(sb, child);
		}
	}
	
	private void addEntry(Entry node) {
		this.entries.put(node.getPath(), node);
		
		Entry parent = getParent(node.getPath().getParent());
		if (parent != null) {
			parent.addChild(node);
		}
	}
	
	private Entry getParent(Path path) {
		if (path == null) {
			return null;
		}
		Entry node = this.entries.get(path);
		
		if (node != null) {
			return node;
		}
		
		node = new Entry(path);
		Entry parentNode = getParent(path.getParent());
		if (parentNode != null) {
			parentNode.addChild(node);
		} else {
			this.entries.get(ROOT).addChild(node);
		}
		
		this.entries.put(path, node);
		
		return node;
	}
}
