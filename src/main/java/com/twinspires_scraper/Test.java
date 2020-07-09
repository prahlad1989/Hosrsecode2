package com.twinspires_scraper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class Test {

	public static void main(String[] args) throws ClientProtocolException, IOException {
		// TODO Auto-generated method stub

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet request = new HttpGet("https://www.twinspires.com/adw/legacy/cdi/integratedscratch?username=my_tux&track=scd&type=Harness&race=7");
		request.addHeader(HttpHeaders.USER_AGENT, getRndUserAgent());
		CloseableHttpResponse response = httpClient.execute(request);
		
		
		HttpEntity entity = response.getEntity();
        if (entity != null) {
            // return it as a String
            String result = EntityUtils.toString(entity);
            System.out.println(result);
        }
        
        httpClient.close();
	}

	public static String getRndUserAgent() {
		
		try {
			List<String> lines = Files.readAllLines(Paths.get("user_agent.txt"));
			
			Random random = new Random();
			
			int rnd = random.nextInt(lines.size());
			
			String userAgent = lines.get(rnd);
			
			System.out.println("userAgent=" + userAgent);
			return userAgent;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";
		
	}
}
