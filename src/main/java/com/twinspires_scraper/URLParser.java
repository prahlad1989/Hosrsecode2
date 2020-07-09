package com.twinspires_scraper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class URLParser {


	public static final Logger log = Logger.getLogger(URLParser.class);
	
	static Map<String,String> mapCodes = new HashMap<String,String>();
	static {
		
		try {
			List<String> lines = Files.readAllLines(Paths.get("codes.txt"));
			for (String line: lines) {
				String parts[] = line.split("	");
				mapCodes.put(parts[1], parts[0]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static String buildInfoURL(String url) {
		
		String parts[] = url.split("/");
		
		String race = parts[parts.length-1];
		String type = parts[parts.length-2];
		String track = parts[parts.length-3];
		//https://www.twinspires.com/adw/legacy/cdi/integratedscratch?username=my_tux&track=st&type=Thoroughbred&race=1&output=json
		log.debug("built infourl: "+String.format("https://www.twinspires.com/adw/legacy/cdi/integratedscratch?username=my_tux&track=%s&type=%s&race=%s&output=json", track, type, race));
		return String.format("https://www.twinspires.com/adw/legacy/cdi/integratedscratch?username=my_tux&track=%s&type=%s&race=%s&output=json", track, type, race);
	}
	
	public static String buildOddsURL(String url) {
		
		String parts[] = url.split("/");
		
		String race = parts[parts.length-1];
		String type = parts[parts.length-2];
		String track = parts[parts.length-3];
		log.debug("built odds url: "+String.format("https://www.twinspires.com/adw/legacy/tote/oddsmtppost?username=my_tux&output=json&track=%s&type=%s&race=%s", track, type, race));
		return String.format("https://www.twinspires.com/adw/legacy/tote/oddsmtppost?username=my_tux&output=json&track=%s&type=%s&race=%s", track, type, race);
	}

	public static boolean isStopped(String url) {
		
		boolean isStopped = false;
		try {
			String parts[] = url.split("/");
			String track = parts[parts.length-3];
			String raceNumber = parts[parts.length-1];
			
			String code = mapCodes.get(track);
			
			String statusURL = "https://www.twinspires.com/adw/track/"+code+"/race";
			String oddsJSON = parseURL(statusURL);
			
			JSONParser jsonParser = new JSONParser();

			JSONArray arr = (JSONArray) jsonParser.parse(oddsJSON);
			
			if (arr!= null) {
				
				for (int i=0;i<arr.size(); i++) {
					JSONObject item = (JSONObject) arr.get(i);
			
					String race = item.get("race").toString();
					
					if (race.equals(raceNumber)) {
						
						String raceStatus = item.get("raceStatus").toString();
						
						if ("Closed".equals(raceStatus)) {
							isStopped = true;
						}
						
						break;
					}
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return isStopped;
		
	}
	
	public static String parseURL(String url) throws IOException {
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		try {
			HttpGet request = new HttpGet(url);
			request.addHeader(HttpHeaders.USER_AGENT, getRndUserAgent());
			CloseableHttpResponse response = httpClient.execute(request);
			
			
			HttpEntity entity = response.getEntity();
	        if (entity != null) {
	            // return it as a String
	            String result = EntityUtils.toString(entity);
	            return result;
	        }
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		} finally {
			httpClient.close();
		}
		
		return null;
	}
	
	public static String getRndUserAgent() {
	
		try {
			List<String> lines = Files.readAllLines(Paths.get("user_agent.txt"));
			
			Random random = new Random();
			
			int rnd = random.nextInt(lines.size());
			
			return lines.get(rnd);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/601.3.9 (KHTML, like Gecko) Version/9.0.2 Safari/601.3.9";
		
	}
	
	public static String getSupposedSheetNameFromURL(String url) {
		String supposedSheetName="";
		String[] tokensOfURL=url.split("/");
		String placeName=tokensOfURL[tokensOfURL.length-4];
		String afterRemovingHyfen=placeName.split("-")[0];
		String raceNum=tokensOfURL[tokensOfURL.length-1];
		supposedSheetName=afterRemovingHyfen+"-"+raceNum;
		//log.debug("expected sheet name"+supposedSheetName);
		return supposedSheetName;
	}
	
	public static void main(String[] args) {
//		String url = buildOddsURL("https://www.twinspires.com/bet/program/scioto-downs/scd/Harness/7");
		
//		System.out.println(url);
		log.debug("sheet name "+getSupposedSheetNameFromURL("https://www.twinspires.com/bet/program/los-alamitos-qh/la/Thoroughbred/8"));
		
		boolean isStp = isStopped("https://www.twinspires.com/bet/program/scioto-downs/scd/Harness/7");
		
		System.out.println(isStp);
	}
}
