package com.twinspires_scraper;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {

	public static Properties prop = new Properties();
	
	static {
		
		try {
			FileInputStream input = new FileInputStream("config.properties");
			prop.load(input);
			input.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getString(String key) {
		return prop.getProperty(key);
	}
	
}
