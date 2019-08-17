package es.german.djtools.rekordboxtoalbum;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.DocumentException;
import org.easybatch.core.job.Job;
import org.easybatch.core.job.JobBuilder;
import org.easybatch.core.job.JobExecutor;
import org.easybatch.core.job.JobReport;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import es.german.djtools.rekordboxtoalbum.rekordbox.RBParser;
import es.german.djtools.rekordboxtoalbum.rekordbox.Track;
import es.german.djtools.rekordboxtoalbum.util.FileRecordWithIgnoreReader;
import es.german.djtools.rekordboxtoalbum.util.Mp3Processor;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

class App {
	
	final static Logger logger = LogManager.getLogger();

	/**
	 * @param args
	 * @throws DocumentException
	 * @throws IOException
	 * @throws InvalidDataException 
	 * @throws UnsupportedTagException 
	 * @throws NotSupportedException 
	 */
	public static void main(String[] args) throws IOException, UnsupportedTagException, InvalidDataException, DocumentException, NotSupportedException {
		new App().run(args);
	}
	
	private void run(String[] args) throws IOException, DocumentException {
		long timeInit = System.currentTimeMillis();
		
		parseArgs(args);

		logger.info("Executing...");
		
		Map<String, Track> rbTracks = null;
		if (Settings.PATH_REKORDBOX_LIBRARY != null) {
			RBParser rbParser = new RBParser();
			rbTracks = rbParser.parser(new File(Settings.PATH_REKORDBOX_LIBRARY));
		} else {
			rbTracks = new HashMap<String, Track>();
		}
		
		Job job = new JobBuilder()
		         .reader(new FileRecordWithIgnoreReader(new File(Settings.PATH_TO_PROCESS), "mp3"))
		         .processor(new Mp3Processor(rbTracks))
		         .batchSize(10)
		         .build();

		JobExecutor jobExecutor = new JobExecutor();
		JobReport report = jobExecutor.execute(job);
		jobExecutor.shutdown();

		long timeFinish = System.currentTimeMillis();
		
		logger.info("Processed Elements: " + Stats.getInstance().getElementsProcessed());
		logger.info("Updated Elements: " + Stats.getInstance().getElementsUpdated());
		
		logger.info("Execute time: " + (timeFinish - timeInit) + "ms.");
	}
	
	private void parseArgs(String[] args) {
		
		logger.info("Parsing arguments...");
		
		ArgumentParser parser = ArgumentParsers.newFor("rekordbox-to-album").build()
                .defaultHelp(true)
                .description("Use the tracks genre, rekordbox rating and comments to fill the Album tag.");
		
		parser.addArgument("-p", "--path").required(true)
			.type(Arguments.fileType().verifyIsDirectory().verifyCanWrite())
			.help("Directory with the mp3 files to process");
		
		parser.addArgument("-rbp", "--rekordboxpath").required(false)
			.type(Arguments.fileType().verifyIsFile().verifyCanRead())
			.help("XML path with the rekordbox library (Rekordbox -> Export -> XML)");
		
		parser.addArgument("-f", "--full")
			.type(Boolean.class).choices(true, false).setDefault(false)
			.help("Full update (Override all files always or only when it detect album changes)");
		
		parser.addArgument("-s", "--save")
			.type(Boolean.class).choices(true, false).setDefault(false)
			.help("Save changes or execute simulation only");

        Namespace ns = null;
 
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
        	logger.error("Error parsing arguments", e);
        	System.out.println("\n\n\n");
            parser.printHelp();
            System.exit(1);
        }
        
        Settings.PATH_TO_PROCESS = ns.getString("path");
        Settings.PATH_REKORDBOX_LIBRARY = ns.getString("rekordboxpath");
        Settings.FULL_UPDATE = ns.getBoolean("full");
        Settings.SAVE_CHANGES = ns.getBoolean("save");
        
	}
	
	
}
