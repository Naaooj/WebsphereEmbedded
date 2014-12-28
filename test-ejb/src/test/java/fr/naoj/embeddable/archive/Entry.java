package fr.naoj.embeddable.archive;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;

/**
 * An entry is the relation between a {@link ZipEntry} of the {@link Archive} and the {@link Path}
 * within it for a given {@link Content}.
 * 
 * @author Johann Bernez
 */
public class Entry {

	private Content content;
	private Path path;
	private List<Entry> children;
	
	public Entry(Path path) {
		this.path = path;
	}
	
	public Entry(Content content, Path path) {
		this(path);
		this.content = content;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Content> T getContent() {
		return (T) content;
	}
	
	public List<Entry> getChildren() {
		if (this.children == null) {
			return Collections.<Entry> emptyList();
		}
		return this.children;
	}
	
	public InputStream asStream() {
		return content.getInputStream();
	}
	
	
	public Path getPath() {
		return this.path;
	}
	
	public void addChild(Entry child) {
		if (this.children == null) {
			this.children = new ArrayList<Entry>(1);
		}
		this.children.add(child);
	}
	
	@Override
	public String toString() {
		return this.getPath().toString();
	}
}
