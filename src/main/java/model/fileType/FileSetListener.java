package model.fileType;

/**
 * Interface for things that want to know when a file set has moved.
 *
 * @author alok
 *
 */
public interface FileSetListener {

	public void onFileSetMoved(FileSet fileset);
}
