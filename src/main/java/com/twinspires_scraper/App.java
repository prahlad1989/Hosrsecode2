package com.twinspires_scraper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteSheetRequest;
import com.google.api.services.sheets.v4.model.DuplicateSheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;


public class App {
	
	public static final Logger log = Logger.getLogger(App.class);
	
	private static final String spreadSheetId = ConfigLoader.getString("Spreadsheet_ID");
	
	private static final String modelName="NewModel";
	
	private static final String indexSheetName="IndexSheet";
	
	private static Sheets service;
	
	public App() throws IOException, GeneralSecurityException {
		service = SheetsServiceUtil.getSheetsService();
	}
	
	Map<String, Boolean> processedURLs = new HashMap<String, Boolean>();
	
	public static void main(String[] args) {

		try {
			new App().process();
		} catch (Exception e) {

			e.printStackTrace();
		}
		
	}
	
	public void createRequiredTabs(List<String> urls) {
		try {
		Spreadsheet response = service.spreadsheets().get(spreadSheetId).execute();
		List<Sheet> sheets = response.getSheets();
		Sheet modelSheet=null;
		for(Sheet sheet:sheets) {
			if(sheet.getProperties().getTitle().equals(modelName)) {
				modelSheet=sheet;
				break;
			}
		}
		urls=new ArrayList<String>(urls);
		Collections.reverse(urls);
		
		List<Request> requests=new ArrayList<Request>();
		for(String url:urls) {
			String supposedSheetName=URLParser.getSupposedSheetNameFromURL(url);
				DuplicateSheetRequest request= new DuplicateSheetRequest();
				request.setSourceSheetId(modelSheet.getProperties().getSheetId());
				request.setNewSheetName(supposedSheetName);
				requests.add(new Request().setDuplicateSheet(request)); //to create sheet that doesn't exist through Batchrequest
		}
		BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest=new BatchUpdateSpreadsheetRequest();
		batchUpdateSpreadsheetRequest.setRequests(requests);
		if(requests.size()>0)
		service.spreadsheets().batchUpdate(spreadSheetId,batchUpdateSpreadsheetRequest).execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	public void process() throws IOException, InterruptedException {

		//Map<String,Sheet> existedSheetNameMap=new HashMap<String,Sheet>();
		List<String> urls = Files.readAllLines(Paths.get("urls.txt"));
		urls=urls.stream().filter(url -> url != null && url.trim().length() > 0).collect(Collectors.toList());
		deleteOldTabs();
		createRequiredTabs(urls);
		Spreadsheet response = service.spreadsheets().get(spreadSheetId).execute();
		
		List<Sheet> sheets = response.getSheets();
		
		  sheets.sort(new Comparator<Sheet>() {
		  public int compare(Sheet o1, Sheet o2) { // TODO Auto-generated
			  return o1.getProperties().getTitle().compareTo(o2.getProperties().getTitle()); } 
		  });
		 
		while (true) {
			
			if (processedURLs.size() == urls.size()) {
				break;
			}
			List<List<Object>> raceDetailsList = new ArrayList<List<Object>>();
			
			for (int i=0; i < urls.size(); i++) {
				
				String url = urls.get(i);
				
				if (sheets.size() > i && !processedURLs.containsKey(url)) {
					
					String supposedSheetName=URLParser.getSupposedSheetNameFromURL(url);
					
					String range = supposedSheetName + "!A1:C";
					List raceDetails = processURL(url, range);
					if(raceDetails != null) {
						raceDetails.add(supposedSheetName);
						raceDetailsList.add(raceDetails);
					}
					//Now do all coloring stuff.
					AppsScriptQuickstart.callScriptPerTab(supposedSheetName);
				}
			}
			//Now update index with latest race timings etc.
			String range=indexSheetName+"!A1:D";
			update(raceDetailsList, range);
			
			System.out.println("Sleep in 5 seconds");
			Thread.sleep(5000);
		}
		
		System.out.println("Done!");
	}

private void deleteOldTabs() {
		try {
			Spreadsheet response = service.spreadsheets().get(spreadSheetId).execute();
			
			List<Sheet> sheets = response.getSheets();
			
			List<Request> requests=new ArrayList<Request>();
			
			for(Sheet sheet:sheets) {
				
				if(!sheet.getProperties().getTitle().equals(modelName) && !sheet.getProperties().getTitle().equals(indexSheetName)) {
					DeleteSheetRequest deleteSheetRequest=new DeleteSheetRequest();
					deleteSheetRequest.setSheetId(sheet.getProperties().getSheetId());
					requests.add(new Request().setDeleteSheet(deleteSheetRequest));
				}
			}
			
			BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest=new BatchUpdateSpreadsheetRequest();
			batchUpdateSpreadsheetRequest.setRequests(requests);
			if(requests.size()>0)
			service.spreadsheets().batchUpdate(spreadSheetId,batchUpdateSpreadsheetRequest).execute();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
	}

	public List processURL(String url, String range) {

		try {
			log.info("Process for url: " + url);

			List<List<Object>> values = new ArrayList<List<Object>>();
			
			String infoURL = URLParser.buildInfoURL(url);
			String oddsURL = URLParser.buildOddsURL(url);
			
			String infoJSON = URLParser.parseURL(infoURL);
			if (infoJSON == null) {
				log.error("Can not read infoURL=" + infoURL);
				return null;
			}
			
			String oddsJSON = URLParser.parseURL(oddsURL);
			if (oddsJSON == null) {
				log.error("Can not read oddsURL=" + oddsURL);
				return null;
			}
			
			JSONParser jsonParser = new JSONParser();
            log.debug("info json "+infoJSON);
            log.debug("odds json" +oddsJSON);
			JSONObject object = (JSONObject) jsonParser.parse(infoJSON);
			
			JSONObject object1 = (JSONObject) jsonParser.parse(oddsJSON);
			
			object = (JSONObject) object.get("IntegratedScratches");

			if (object == null || object1 == null) {
				
				log.error("Empty json");
				return null;
			}
			
			String displayName = (String) object.get("DisplayName");
			
			if (displayName == null) {
				log.error("Race is no longer existing");
				
				processedURLs.put(url, true);
				return null;
			}
			
			JSONObject mtpInfo = (JSONObject) object1.get("MtpInfo");
			
			String mtpStr = mtpInfo.get("Mtp").toString();
			
			String mtp = "";
//			if ("0".equals(mtpStr)) {
//				mtp = "Offical";
//				processedURLs.put(url, count++);
//				
//				if (processedURLs.get(url) == 20) {
//					mtp = "Offical";
//				}
//			} else {
//				mtp = mtpStr + " MTP";
//			}
			
//			if (Integer.parseInt(mtpStr) <= 5) {
//				
//			}
			
			boolean isStopped = URLParser.isStopped(url);
			if (isStopped) {
				mtp = "Off";
				processedURLs.put(url, true);
			} else {
				mtp = mtpStr + " MTP";
			}
			
			String race = "Race " + object.get("Race").toString() + " - " + mtp;
			
			JSONArray jsonArray = (JSONArray) object.get("EntryChanges");
			
			if (jsonArray == null) return null;
			
			JSONObject winOdds = (JSONObject) object1.get("WinOdds");
			JSONArray entries = (JSONArray) winOdds.get("Entries");
			
			List row1 = Arrays.asList(displayName, race, getCurrentDate());
			values.add(row1);
			log.debug("Title row"+row1);
			List row2 = Arrays.asList(getCurrentDate(), null, null);
			List row3 = Arrays.asList(null, null, null);
			List row4 = Arrays.asList(null, null, null);
			values.add(row2);
			values.add(row3);
			values.add(row4);
			
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject item = (JSONObject) jsonArray.get(i);
				
				if (entries.size() > i) {
					JSONObject entry = (JSONObject) entries.get(i);
					
					String numOdds = entry.get("NumOdds").toString();
					
					String programNumber = item.get("ProgramNumber").toString();
					String horseName = item.get("HorseName").toString();
					
					
					List row = Arrays.asList(programNumber,horseName, numOdds);
					values.add(row);
				}
			}
			
			for (int i = 0; i < 10; i++) {
				List rowx = Arrays.asList("", "", "");
				values.add(rowx);
			}

		    update(values, range);
		    return row1;
		    
		    
		    
		    
		    
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}
		return null;
	}
	

	

    public static void update(List<List<Object>> values, String outputRange) throws IOException {
		
		log.info("update...");
		
//	    List<List<Object>> values = Arrays.asList(bids);
	    ValueRange valueRange = new ValueRange();
		valueRange.setValues(values);

		UpdateValuesResponse response =
	    		service.spreadsheets().values().update(spreadSheetId, outputRange, valueRange)
	    		.setValueInputOption("USER_ENTERED")
	    		
	    		.execute();
		
		//service.spreadsheets().values().
		
		log.info(response);

	}
    
    public static String getCurrentDate() {
    	
    	SimpleDateFormat dateFormat = new SimpleDateFormat("M/dd/yyyy");
    	return dateFormat.format(new Date());
    }
}
