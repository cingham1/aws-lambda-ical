package net.cingham.ical;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class APIGatewayProxyHandler {

	private static ICalHostingRelay iCalRelay = new ICalHostingRelay();

	public static final String BLANK_URI = "/";
	public static final String HEALTH_URI = "/health";
	public static final String TYPE_PATH_PARAM = "type";
	
	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String MIME_TYPE_CALENDAR = "text/calendar; charset=utf-8";
	public static final String MIME_TYPE_PROBLEM_JSON = "application/problem+json";
	public static final String MIME_TYPE_PLAIN_TEXT = "text/plain";
	public static final String HEADER_CACHE_CONTROL = "Cache-Control";
	public static final String HEADER_CACHE_CONTROL_VAL = "private";
	public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
	public static final String HEADER_CONTENT_DISPOSITION_VAL = "inline; filename=hosting.ics";

	private static Properties properties = loadProperties();

	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
		LambdaLogger logger = context.getLogger();
		logger.log("** received request : " + request);

		if (StringUtils.equals(request.getPath(), HEALTH_URI) ||
				StringUtils.equals(request.getPath(), BLANK_URI)) {
			APIGatewayProxyResponseEvent response = createHealthResponse();
			logger.log("health check: " + response);
			return response;
		}

		String strResults;
		try {
			// type will come from API Gateway url, e.g. /hosting-relay/{type}
			String type = null;
			if (request.getPathParameters() != null) {
				type = request.getPathParameters().get(TYPE_PATH_PARAM);
			}
			type = (type == null) ? "(unknown)" : type;

			strResults = iCalRelay.getICalRelay(type, context);

		} catch (IllegalArgumentException iae) {
			logger.log("** error : " + iae.getMessage());
			return createErrorResponse(HttpURLConnection.HTTP_BAD_REQUEST, iae.getMessage());
		} catch (Exception ex) {
			logger.log("** error : " + ex.getMessage());
			logger.log(ExceptionUtils.getStackTrace(ex));
			return createErrorResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, ex.getMessage());
		}

		APIGatewayProxyResponseEvent response = createResponse(HttpURLConnection.HTTP_OK, 
				MIME_TYPE_CALENDAR, strResults);

		logger.log("** sending response : " + response);
		return response;
	}

	private APIGatewayProxyResponseEvent createHealthResponse() {
		String body = properties.getProperty("application.name") + "\n" +
				properties.getProperty("application.description") + "\n" +
				"Version: " + properties.getProperty("application.version") + "\n" +
				"Build: " + properties.getProperty("build.timestamp") + "\n\n" +
				"Health: OK\n";
		return createResponse(HttpURLConnection.HTTP_OK, MIME_TYPE_PLAIN_TEXT, body);
	}

	private APIGatewayProxyResponseEvent createErrorResponse(int status, String message) {
		String body = "{ \"status\":" + status + ", \"message\":\"" + message + "\" }";
		return createResponse(status, MIME_TYPE_PROBLEM_JSON, body);
	}

	private APIGatewayProxyResponseEvent createResponse(int status, String mimeType, String body) {
		Map<String, String> headers = new HashMap<>();
		headers.put(HEADER_CONTENT_TYPE, mimeType);
		if (mimeType.equals(MIME_TYPE_CALENDAR)) {
			headers.put(HEADER_CACHE_CONTROL, HEADER_CACHE_CONTROL_VAL);
			headers.put(HEADER_CONTENT_DISPOSITION, HEADER_CONTENT_DISPOSITION_VAL);
		}

		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		response.setStatusCode(status);
		response.setHeaders(headers);
		response.setBody(body);

		return response;
	}

	private static Properties loadProperties() {
		Properties properties = new Properties();
		try {
			InputStream inStream = APIGatewayProxyHandler.class.getResourceAsStream("/application.properties");
			properties.load(inStream);
			inStream.close();
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
			throw new RuntimeException(ioe);
		}
		return properties;
	}
}
