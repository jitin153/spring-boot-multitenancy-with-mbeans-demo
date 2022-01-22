package com.demo.multitenancy.exception;

public class DataSourceRoutingException extends RuntimeException {

	private static final long serialVersionUID = 1889607799794461899L;

	public DataSourceRoutingException() {
		super();
	}

	public DataSourceRoutingException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DataSourceRoutingException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataSourceRoutingException(String message) {
		super(message);
	}

	public DataSourceRoutingException(Throwable cause) {
		super(cause);
	}

}
