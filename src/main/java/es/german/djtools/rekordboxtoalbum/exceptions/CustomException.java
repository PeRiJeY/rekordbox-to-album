package es.german.djtools.rekordboxtoalbum.exceptions;

public class CustomException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8131013879888853598L;

	public CustomException(String errorMessage) {
		super(errorMessage);
	}

}
