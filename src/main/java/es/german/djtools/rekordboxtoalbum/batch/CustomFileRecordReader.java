package es.german.djtools.rekordboxtoalbum.batch;

import static java.lang.String.format;
import static org.easybatch.core.util.Utils.checkArgument;
import static org.easybatch.core.util.Utils.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easybatch.core.reader.RecordReader;
import org.easybatch.core.record.FileRecord;
import org.easybatch.core.record.Header;

public class CustomFileRecordReader implements RecordReader {
	
	final static Logger logger = LogManager.getLogger();
	
	private File directory;
    private Iterator<File> iterator;
    private long currentRecordNumber;
    private String[] extensions;

    /**
     * Create a new {@link CustomFileRecordReader}.
     *
     * @param directory to read files from
     * @param extensions a list of extensions. Example: .log
     */
    public CustomFileRecordReader(final File directory, String... extensions) {
    	checkNotNull(directory, "directory");
        this.directory = directory;
        this.extensions = extensions;
    }
    
    public void open() throws Exception {
        checkDirectory();
        iterator = getFiles(directory).listIterator();
        currentRecordNumber = 0;
    }

    private List<File> getFiles(final File directory) throws IOException {
        int maxDepth = Integer.MAX_VALUE;
        FilesCollector filesCollector = new FilesCollector(extensions);
        EnumSet<FileVisitOption> fileVisitOptions = EnumSet.noneOf(FileVisitOption.class);
        Files.walkFileTree(directory.toPath(), fileVisitOptions, maxDepth, filesCollector);
        return filesCollector.getFiles();
    }

    private void checkDirectory() {
        checkArgument(directory.exists(), format("Directory %s does not exist.", getDataSourceName()));
        checkArgument(directory.isDirectory(), format("%s is not a directory.", getDataSourceName()));
        checkArgument(directory.canRead(), format("Unable to read files from directory %s. Permission denied.", getDataSourceName()));
    }

    public FileRecord readRecord() {
        Header header = new Header(++currentRecordNumber, getDataSourceName(), new Date());
        if (iterator.hasNext()) {
            return new FileRecord(header, iterator.next());
        } else {
            return null;
        }
    }

    private String getDataSourceName() {
        return directory.getAbsolutePath();
    }

    public void close() {
        // no op
    }
	
	

}
