/**
 * 
 */
package es.german.djtools.rekordboxtoalbum.rekordbox;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

/**
 * @author Germán
 *
 */
public class RBParser {
	
	final static Logger logger = LogManager.getLogger();

	public Map<String, Track> parser(final File fileXML) throws MalformedURLException, DocumentException, UnsupportedEncodingException {
		
		Document d = loadFile(fileXML);
		Map<String, Track> result = loadTracks(d);
		
		logger.debug(d.asXML());
		
		return result;
	}
	
	public Document loadFile(File file) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(file);
        return document;
    }
	
	public Map<String, Track> loadTracks(Document document) throws UnsupportedEncodingException {
		Map<String, Track> result = new HashMap<String, Track>();
		List<Node> list = document.selectNodes("DJ_PLAYLISTS/COLLECTION/TRACK");
		
		for (Iterator<Node> iter = list.iterator(); iter.hasNext();) {
			Node node = (Node) iter.next();
			Track t = new Track();
			t.setLocation(node.valueOf("@Location"));
			t.setFileName(locationToFilename(t.getLocation()));
			t.setRating(Integer.parseInt(node.valueOf("@Rating")));
			t.setBpm(node.valueOf("@AverageBpm"));
			
			t.setListCues(parseCues(node));
			
			logger.debug(t.getFileName());
			result.put(t.getFileName(), t);
	    }
		
		return result;
	}
	
	private List<RBCue> parseCues(Node nodeTrack) {
		List<RBCue> results = new ArrayList<RBCue>();
		List<Node> lNodesCue = nodeTrack.selectNodes("POSITION_MARK");
		
		for (Iterator<Node> iter = lNodesCue.iterator(); iter.hasNext();) {
			Node node = (Node) iter.next();
			
			RBCue cue = new RBCue();
			cue.setPosition(Integer.parseInt(node.valueOf("@Num")));
			cue.setType(Integer.parseInt(node.valueOf("@Type")));
			cue.setStart(Float.parseFloat(node.valueOf("@Start")));
			
			if (cue.getType() == 4) {
				cue.setEnd(Float.parseFloat(node.valueOf("@End")));				
			}
			
			results.add(cue);
		}
		return results;
	}
	
	public String locationToFilename(String location) throws UnsupportedEncodingException {
		String fileName = location.replace("file://localhost/", "");
		return java.net.URLDecoder.decode(fileName, "UTF-8");
	}

}
