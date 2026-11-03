package com.movie.booking.gateway_service;

import java.util.regex.Pattern;

/** Header name and accepted format; identical to the RequestIdFilter in the servlet services. */
public final class RequestId {

	public static final String HEADER = "X-Request-Id";
	public static final String ATTRIBUTE = "requestId";
	public static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

	private RequestId() {
	}

}
