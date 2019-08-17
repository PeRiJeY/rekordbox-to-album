package es.german.djtools.rekordboxtoalbum.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileManager {
	
	final static Logger logger = LogManager.getLogger();

	public static List<File> loadFilesMp3Folder(final File folder) {

		List<File> results = new ArrayList<File>();
		
		logger.debug("Exploring... " + folder.getPath());

		for (final File f : folder.listFiles()) {
			if (f.isDirectory() && !checkIgnore(f)) {
				results.addAll(loadFilesMp3Folder(f));
			} else if (StringUtils.equalsIgnoreCase("mp3", getFileExtension(f))) {
				logger.debug("Adding to list files: " + f.getName());
				results.add(f);
			}
		}

		return results;

	}

	private static String getFileExtension(File file) {
		String fileName = file.getName();
		if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
			return fileName.substring(fileName.lastIndexOf(".") + 1);
		else
			return "";
	}
	
	private static boolean checkIgnore(File directory) {
		File fileIgnore = new File(directory.getPath() + "/.ignoreFolder");
		if (fileIgnore.exists()) logger.info("Ignoring " + directory.getPath());
		
		return fileIgnore.exists();
	}

}
