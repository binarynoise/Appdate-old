package de.binarynoise.appdate.util;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

public class GoogleSheetsBridge {
	private static final String           applicationName = "Appdate";
	private static final String           spreadsheetId   = "1cq4gZLevu0hPIcaP6Z4Hp46hrmH47R7Zd83XSNNKp3g";
	private static final String           range           = "templates!A2:C1000";
	private static final HttpTransport    httpTransport;
	private static final JsonFactory      jsonFactory     = JacksonFactory.getDefaultInstance();
	private static final Sheets           sheets;
	private static       GoogleCredential credential;
	
	static {
		try {
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			authExplicit();
			sheets = getSheets();
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	@RunInBackground
	public static List<List<Object>> getValues() throws IOException {
		return getValueRange().getValues();
	}
	
	@RunInBackground
	public static void updateValues(List<List<Object>> newValues) throws IOException {
		ValueRange valueRange = new ValueRange();
		valueRange.setValues(newValues);
		updateValueRange(valueRange);
	}
	
	private static void authExplicit() throws IOException {
		List<String> scopes = new ArrayList<>();
		scopes.add("https://www.googleapis.com/auth/cloud-platform");
		scopes.addAll(SheetsScopes.all());
		credential = jsonFactory.fromString("", GoogleCredential.class).createScoped(scopes);
		throw new IOException("No api key, get yourself your own api key and put it here");
	}
	
	@RunInBackground
	private static Sheets getSheets() {
		return new Sheets.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName).build();
	}
	
	@RunInBackground
	private static ValueRange getValueRange() throws IOException {
		return sheets.spreadsheets().values().get(spreadsheetId, range).execute();
	}
	
	@RunInBackground
	private static void updateValueRange(ValueRange valueRange) throws IOException {
		sheets.spreadsheets().values().update(spreadsheetId, range, valueRange).execute();
	}
}
