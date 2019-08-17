package es.german.djtools.rekordboxtoalbum;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.DocumentException;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import es.german.djtools.rekordboxtoalbum.rekordbox.RBParser;
import es.german.djtools.rekordboxtoalbum.rekordbox.Track;
import es.german.djtools.rekordboxtoalbum.util.FileManager;
import es.german.djtools.rekordboxtoalbum.util.Mp3Manager;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

class Main {
	
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

		long timeInit = System.currentTimeMillis();
		
		parseArgs(args);

		logger.info("Executing...");
		
		// String pathRekordboxLibrary = "E:/Desarrollos/DjTools/rekordbox-to-traktor/src/main/resources/examples/ExportEnXML_15082019.xml";
		
		// String pathFolderToProcess = "E:/tmp/";
		// pathFolderToProcess = "F:/Musica/Reggaeton/";
		// pathFolderToProcess = "F:/Musica/Electronica/Techno/";
		// pathFolderToProcess = "F:/Musica/Electronica/";
		// pathFolderToProcess = "F:/Musica/";
		
		Map<String, Track> rbTracks = null;
		if (Settings.PATH_REKORDBOX_LIBRARY != null) {
			RBParser rbParser = new RBParser();
			rbTracks = rbParser.parser(new File(Settings.PATH_REKORDBOX_LIBRARY));
		} else {
			rbTracks = new HashMap<String, Track>();
		}
		
		List<File> lFiles = FileManager.loadFilesMp3Folder(new File(Settings.PATH_TO_PROCESS));
		
		logger.info("Files to process: " + lFiles.size());
		
		Mp3Manager.processFiles(lFiles, rbTracks);

		long timeFinish = System.currentTimeMillis();
		
		logger.info("Processed Elements: " + Stats.getInstance().getElementsProcessed());
		logger.info("Updated Elements: " + Stats.getInstance().getElementsUpdated());
		
		logger.info("Execute time: " + (timeFinish - timeInit) + "ms.");
	}
	
	private static void parseArgs(String[] args) {
		
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
