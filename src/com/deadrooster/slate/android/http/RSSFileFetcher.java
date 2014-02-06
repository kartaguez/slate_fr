package com.deadrooster.slate.android.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RSSFileFetcher {

	private static RSSFileFetcher instance;

	private RSSFileFetcher() {
	}

	public static RSSFileFetcher getInstance() {
		
		if (instance == null) {
			synchronized (RSSFileFetcher.class) {
				if (instance == null) {
					RSSFileFetcher.instance = new RSSFileFetcher();
				}
			}
		}

		return RSSFileFetcher.instance;
	}

	public String fetch(String urlString) throws MalformedURLException {

		String content = null;

		URL url = new URL(urlString);
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			InputStream in = new BufferedInputStream(connection.getInputStream());
			content = readStream(in);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return content;
	}

	private String readStream(InputStream in) {

		StringBuffer sb = new StringBuffer();

		BufferedReader reader = null;

		reader = new BufferedReader(new InputStreamReader(in));

		String line = null;
		try {
			line = reader.readLine();
			while (line != null) {
				sb.append(line);
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();
	}

}
