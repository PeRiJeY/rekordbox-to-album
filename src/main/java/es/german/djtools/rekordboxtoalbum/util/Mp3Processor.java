package es.german.djtools.rekordboxtoalbum.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easybatch.core.processor.RecordProcessor;
import org.easybatch.core.record.FileRecord;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import es.german.djtools.rekordboxtoalbum.Settings;
import es.german.djtools.rekordboxtoalbum.Stats;
import es.german.djtools.rekordboxtoalbum.exceptions.CustomException;
import es.german.djtools.rekordboxtoalbum.rekordbox.Track;

public class Mp3Processor implements RecordProcessor<FileRecord, FileRecord> {
	private static final String RETAG_EXTENSION = ".retag";
	private static final String BACKUP_EXTENSION = ".bak";
	
	final static Logger logger = LogManager.getLogger();
	
	private Map<String, Track> rbTracks = null;
	
	public Mp3Processor(Map<String, Track> rbTracks) {
		super();
		this.rbTracks = rbTracks;
	}

	public FileRecord processRecord(FileRecord record) throws Exception {
		long counter = Stats.getInstance().addElementProcessed();
		if (counter % 20 == 0) {			
			logger.debug("Proccesed: " + counter + " items");
			System.out.printf("Proccesed: " + counter + " items\r", counter);
		}
		
		File f = record.getPayload();

		try {
			
			fillAlbum(f);
		} catch (UnsupportedTagException e) {
			logger.error(f.getName(), e);
		} catch (InvalidDataException e) {
			logger.error(f.getName(), e);
		} catch (NotSupportedException e) {
			logger.error(f.getName(), e);
		} catch (IOException e) {
			logger.error(f.getName(), e);
		} catch (Exception e) {
			logger.error(f.getName(), e);
		}
		
		return record;
	}
	
	private void fillAlbum(final File file)
			throws UnsupportedTagException, InvalidDataException, IOException, NotSupportedException {
		if (file.isFile()) {
			Mp3File mp3file = new Mp3File(file);

			if (mp3file.hasId3v2Tag()) {
				ID3v2 id3v2Tag = mp3file.getId3v2Tag();
				
				logger.debug("File: " + mp3file.getFilename());
				logger.debug("Genre: " + id3v2Tag.getGenreDescription());
				logger.debug("Comment: " + id3v2Tag.getComment());
				logger.debug("AlbumArtist: " + id3v2Tag.getAlbumArtist());
								
				// Process only bad formats
				String previousAlbum = id3v2Tag.getAlbum();
				// if (!Settings.FULL_UPDATE && isCorrectFormat(previousAlbum)) return;

				// Get Track from Recordbox database
				Track rbTrack = rbTracks.get(mp3file.getFilename().replace('\\', '/'));

				// Prepare Genre
				String compactGenre = getCompactGenre(id3v2Tag.getGenreDescription());

				// Prepare Energy
				String compactEnergy = getEnergyFromComment(id3v2Tag.getComment());

				// Prepare Rating
				int compactRating = getRating(rbTrack);

				// Prepare Final String
				if (StringUtils.isNotEmpty(compactGenre)) {
					String finalStr = "g" + compactGenre + "-R" + compactRating + "-E" + compactEnergy;
					logger.debug("Final String: " + finalStr + " (" + mp3file.getFilename() + ")");
					
					id3v2Tag.setAlbum(finalStr);
					id3v2Tag.setAlbumArtist(null);
					id3v2Tag.setTrack("0");
					
					if (Settings.SAVE_CHANGES
							&& (Settings.FULL_UPDATE || !StringUtils.equals(previousAlbum, finalStr))) {
						Stats.getInstance().addElementUpdated();
						saveChanges(mp3file);
					}
				}

			} else {
				throw new CustomException("Not found ID3v2 Tags: " + file.getName());
			}
		} else {
			throw new CustomException("File not valid: " + file.getName());
		}

	}
	
	private String getCompactGenre(String genre) {
		String compactGenre = null;
		if (StringUtils.isNotEmpty(genre)) {
			compactGenre = getCompactGenreOnlyCase(genre);
			if (StringUtils.isEmpty(genre) || StringUtils.length(compactGenre) <= 1) {
				compactGenre = getCompactGenreFirstChars(genre);
			}
		}
		return compactGenre;

	}

	private String getCompactGenreOnlyCase(String genre) {
		String compactGenre = null;
		if (StringUtils.isNotEmpty(genre)) {
			compactGenre = genre.replaceAll("[^A-Z]+", "");
		}
		return compactGenre;
	}
	
	private String getCompactGenreFirstChars(String genre) {
		String compactGenre = null;
		if (StringUtils.isNotEmpty(genre)) {
			compactGenre = genre.length() <= 3 ? genre : genre.substring(0,  3);
		}
		return compactGenre;
	}
	
	/**
	 * Not used for now
	 * @param genre
	 * @return
	 */
	@SuppressWarnings("unused")
	private String getCompactGenreSmart(String genre) {
		String compactGenre = null;
		if (StringUtils.isNotEmpty(genre)) {
			StringBuffer sb = new StringBuffer();
			String split[] = StringUtils.split(genre, " ");
			for (String s : split) {
				sb.append(s.length() <= 3 ? s : s.substring(0,  3));
			}
			compactGenre = sb.toString();
		}
		return compactGenre;
	}

	private int getRating(Track rbTrack) {
		if (rbTrack == null) {
			return 0;
		}

		return convertRating(rbTrack.getRating());
	}

	private int convertRating(int rbRating) {
		Map<Integer, Integer> mapRbTraktor = new HashMap<Integer, Integer>();
		mapRbTraktor.put(255, 5);
		mapRbTraktor.put(204, 4);
		mapRbTraktor.put(153, 3);
		mapRbTraktor.put(102, 2);
		mapRbTraktor.put(51, 1);

		Integer result = mapRbTraktor.get(rbRating);
		return result != null ? result.intValue() : 0;
	}

	private String getEnergyFromComment(String comment) {
		String result = "D";
		if (StringUtils.isNotEmpty(comment)) {
			if (StringUtils.containsIgnoreCase(comment, "/* High */")) {
				result = "H";
			} else if (StringUtils.containsIgnoreCase(comment, "/* Middle */")) {
				result = "M";
			} else if (StringUtils.containsIgnoreCase(comment, "/* Low */")) {
				result = "L";
			}
		}
		return result;
	}
	
	private void saveChanges(Mp3File mp3file) throws NotSupportedException, IOException {
		removeFile(mp3file.getFilename() + RETAG_EXTENSION);
		mp3file.save(mp3file.getFilename() + RETAG_EXTENSION);
		
		renameFiles(mp3file.getFilename());
		removeFile(mp3file.getFilename() + BACKUP_EXTENSION);
	}
	
	private void renameFiles(String filename) throws IOException {
		File originalFile = new File(filename);
		File backupFile = new File(filename + BACKUP_EXTENSION);
		File retaggedFile = new File(filename + RETAG_EXTENSION);
		if (backupFile.exists()) {
			if (!backupFile.delete()) {
				throw new IOException("Error removing backup: " + filename);
			}
		}
		if (!originalFile.renameTo(backupFile)) {
			throw new IOException("Error renaming original file to retag bak extension: " + filename);
		}
		if (!retaggedFile.renameTo(originalFile)) {
			throw new IOException("Error renaming retagged file to original name: " + filename);
		}
	}
	
	private void removeFile(String filename) throws IOException {
		File file = new File(filename);
		if (file.exists()) {
			if (!file.delete()) {
				throw new IOException("Error removing file: " + filename);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private boolean isCorrectFormat(String str) {
		if (str == null) return false;
		
		String regex = "^([gG][A-Za-z]+-R[0-5]-E[A-Z])$";

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);

		return matcher.find();
	}
}
