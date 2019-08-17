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
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;

import es.german.djtools.rekordboxtoalbum.Settings;
import es.german.djtools.rekordboxtoalbum.Stats;
import es.german.djtools.rekordboxtoalbum.exceptions.CustomException;
import es.german.djtools.rekordboxtoalbum.rekordbox.Track;

public class Mp3Processor implements RecordProcessor<FileRecord, FileRecord> {
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
		} catch (Exception e) {
			logger.error(f.getName(), e);
		}
		
		return record;
	}
	
	private void fillAlbum(final File file)
			throws IOException, CannotReadException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		if (file.isFile()) {
			MP3File mp3file = (MP3File) AudioFileIO.read(file);

			if (mp3file.hasID3v2Tag()) {
				AbstractID3v2Tag id3v2Tag = mp3file.getID3v2Tag();
				
				logger.debug("File: " + file.getPath());
				logger.debug("Genre: " + id3v2Tag.getFirst(FieldKey.GENRE));
				logger.debug("Comment: " + id3v2Tag.getFirst(FieldKey.COMMENT));
				logger.debug("AlbumArtist: " + id3v2Tag.getFirst(FieldKey.ALBUM_ARTIST));
								
				// Process only bad formats
				String previousAlbum = id3v2Tag.getFirst(FieldKey.ALBUM);
				// if (!Settings.FULL_UPDATE && isCorrectFormat(previousAlbum)) return;

				// Get Track from Recordbox database
				Track rbTrack = rbTracks.get(file.getPath().replace('\\', '/'));

				// Prepare Genre
				String compactGenre = getCompactGenre(id3v2Tag.getFirst(FieldKey.GENRE));

				// Prepare Energy
				String compactEnergy = getEnergyFromComment(id3v2Tag.getFirst(FieldKey.COMMENT));

				// Prepare Rating
				int compactRating = getRating(rbTrack);

				// Prepare Final String
				if (StringUtils.isNotEmpty(compactGenre)) {
					String finalStr = "g" + compactGenre + "-R" + compactRating + "-E" + compactEnergy;
					logger.debug("Final String: " + finalStr + " (" + file.getPath() + ")");
					
					id3v2Tag.setField(FieldKey.ALBUM, finalStr);
					id3v2Tag.deleteField(FieldKey.ALBUM_ARTIST);
					id3v2Tag.deleteField(FieldKey.TRACK);
					
					if (Settings.SAVE_CHANGES
							&& (Settings.FULL_UPDATE || !StringUtils.equals(previousAlbum, finalStr))) {
						Stats.getInstance().addElementUpdated();
						AudioFileIO.write(mp3file);
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
	
	@SuppressWarnings("unused")
	private boolean isCorrectFormat(String str) {
		if (str == null) return false;
		
		String regex = "^([gG][A-Za-z]+-R[0-5]-E[A-Z])$";

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);

		return matcher.find();
	}
}
