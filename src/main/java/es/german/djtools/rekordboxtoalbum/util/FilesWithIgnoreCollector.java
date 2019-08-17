package es.german.djtools.rekordboxtoalbum.util;

import static java.nio.file.Files.isRegularFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FilesWithIgnoreCollector implements FileVisitor<Path> {
	
	final static Logger logger = LogManager.getLogger();

	private final static String IGNORE_DIRECTORY = ".ignoreFolder";
	
    private List<File> files = new ArrayList<File>();
    private String[] extensions;

    public FilesWithIgnoreCollector(String... extensions) {
    	this.extensions = extensions;
    }

    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return checkIgnore(dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
    }

    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (isRegularFile(file) && checkExtension(file)) {
            files.add(file.toFile());
        }
        return FileVisitResult.CONTINUE;
    }

    public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
        if (exception != null) {
            throw exception;
        }
        return FileVisitResult.CONTINUE;
    }

    public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
        if (exception != null) {
            throw exception;
        }
        return FileVisitResult.CONTINUE;
    }

    public List<File> getFiles() {
        return files;
    }
    
    private boolean checkIgnore(Path directory) {
		File fileIgnore = new File(directory.toString() + "/" + IGNORE_DIRECTORY);
		if (fileIgnore.exists()) logger.info("Ignoring directory " + directory.toString());
		
		return fileIgnore.exists();
	}
    
    private boolean checkExtension(Path file) {
        for (String extension : extensions) {
            if (file.toString().endsWith(extension)) {
                return true;
            }
        }
        logger.info("Ignoring file by extension " + file.toString());
        return false;
    }
}
