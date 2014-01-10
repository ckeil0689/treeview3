package Cluster;

import java.io.File;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ClusterFileFilter extends FileFilter {

	private final FileFilter filter = new FileNameExtensionFilter(
			"Tab-Delim Files", "txt", "tsv", "cdt");

	@Override
	public boolean accept(final File f) {

		return filter.accept(f);
	}

	@Override
	public String getDescription() {

		return "Tab-Delimited Files";
	}
}
