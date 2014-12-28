package fr.naoj.embeddable.archive;

/**
 * Represent the path of an {@link Entry} in an {@link Archive}.
 * 
 * @author Johann Bernez
 */
public class Path {
	
	private String path;
	
	public Path(String path) {
		this.path = path;
	}
	
	public String get() {
		return this.path;
	}
	
	public Path getParent() {
		int lastIndex = this.path.lastIndexOf("/");
		if (lastIndex == -1 || lastIndex == 0) {
			return null;
		}
		return new Path(this.path.substring(0, lastIndex));
	}
	
	@Override
	public String toString() {
		return this.path;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Path)) {
			return false;
		}
		Path p = (Path) o;
		return this.path.equals(p.path);
	}
	
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}
}
