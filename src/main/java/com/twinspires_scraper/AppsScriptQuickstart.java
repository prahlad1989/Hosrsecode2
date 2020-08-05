package com.twinspires_scraper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.script.Script;
import com.google.api.services.script.model.ExecutionRequest;
import com.google.api.services.script.model.Operation;

/**
 * Create a HttpRequestInitializer from the given one, except set the HTTP read
 * timeout to be longer than the default (to allow called scripts time to
 * execute).
 *
 * @param {HttpRequestInitializer} requestInitializer the initializer to copy
 *                                 and adjust; typically a Credential object.
 * @return an initializer with an extended read timeout.
 */
class AppsScriptQuickstart {

	private static final String APPLICATION_NAME = "Apps Script API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
		return new HttpRequestInitializer() {
			@Override
			public void initialize(HttpRequest httpRequest) throws IOException {
				requestInitializer.initialize(httpRequest);
				// This allows the API to call (and avoid timing out on)
				// functions that take up to 6 minutes to complete (the maximum
				// allowed script run time), plus a little overhead.
				httpRequest.setReadTimeout(380000);
			}
		};
	}

	/**
	 * Build and return an authorized Script client service.
	 *
	 * @param {Credential} credential an authorized Credential object
	 * @return an authorized Script client service
	 */
	public static Script getScriptService() throws IOException {
		Credential credential = null;
		try {
			credential = GoogleAuthorizeUtil.authorize();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block253071101817
			e.printStackTrace();
		}
		return new Script.Builder(SheetsQuickstart.HTTP_TRANSPORT, JSON_FACTORY, setHttpTimeout(credential))
				.setApplicationName(APPLICATION_NAME).build();
	}

	/**
	 * Interpret an error response returned by the API and return a String summary.
	 *
	 * @param {Operation} op the Operation returning an error response
	 * @return summary of error response, or null if Operation returned no error
	 */
	public static String getScriptError(Operation op) {
		if (op.getError() == null) {
			return null;
		}

		// Extract the first (and only) set of error details and cast as a Map.
		// The values of this map are the script's 'errorMessage' and
		// 'errorType', and an array of stack trace elements (which also need to
		// be cast as Maps).
		Map<String, Object> detail = op.getError().getDetails().get(0);
		List<Map<String, Object>> stacktrace = (List<Map<String, Object>>) detail.get("scriptStackTraceElements");

		java.lang.StringBuilder sb = new StringBuilder("\nScript error message: ");
		sb.append(detail.get("errorMessage"));
		sb.append("\nScript error type: ");
		sb.append(detail.get("errorType"));

		if (stacktrace != null) {
			// There may not be a stacktrace if the script didn't start
			// executing.
			sb.append("\nScript error stacktrace:");
			for (Map<String, Object> elem : stacktrace) {
				sb.append("\n  ");
				sb.append(elem.get("function"));
				sb.append(":");
				sb.append(elem.get("lineNumber"));
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	public static void main(String[] args) throws IOException {
		 callScriptPerTab("hk-4");
	}

	public static void callScriptPerTab(String tabName) throws IOException {
		// ID of the script to call. Acquire this from the Apps Script editor,
		// under Publish > Deploy as API executable.
		String scriptId = "1KyGmb0gMLLhMU9yrNJ6SeBZ7yzI9WM8YuX-ONbdvHD0-BeDKZ_arE-Zt";
		Script service = getScriptService();

		// Create an execution request object.
		// ExecutionRequest request = new ExecutionRequest()
		// .setFunction("colorAll");//.setpasetParameters(Arrays.asList(new Object[]
		// {"Copy of ok1 1"}));
		ExecutionRequest request = new ExecutionRequest().setFunction("colorSpecifiedSheet")
				.setParameters(Arrays.asList(new Object[] { tabName }));
		//ExecutionRequest request =new ExecutionRequest().setFunction("myFunction");
		try {
			// Make the API request.
			Operation op = service.scripts().run(scriptId, request).execute();
			// Print results of request.
			if (op.getError() != null) {
				// The API executed, but the script returned an error.
				System.out.println(getScriptError(op));
			} else {
				System.out.println(op.getResponse().get("result").toString());

			}
		} catch (GoogleJsonResponseException e) {
			// The API encountered a problem before the script was called.
			e.printStackTrace(System.out);
		}
	}
}