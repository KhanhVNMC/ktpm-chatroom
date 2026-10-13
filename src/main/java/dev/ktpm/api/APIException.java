package dev.ktpm.api;

public class APIException extends RuntimeException {
	private static final long serialVersionUID = -2547902446404002318L;
	
	public final int status;
	
	public APIException(int status, String message) {
		super(message);
		this.status = status;
	}
}