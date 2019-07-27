package net.cingham.ical.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import net.cingham.ical.domain.SiteInfo;

/**
 * 
 * Make REST call to get raw data (as string) from an ICal server. 
 * This is defined as a separate class to facilitate Mock retrieval in unit tests.
 *
 * @author cingham
 *
 */
public class ICalLoader {

	public ICalLoader() {

	}

	public String loadICalData(SiteInfo siteInfo) throws IOException {
		URL url = new URL(siteInfo.getUrl());
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "text/calendar");

		if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new IOException("Failed call to [" + siteInfo.getUrl() + 
					"]: HTTP error code : " + conn.getResponseCode());
		}

		String results = IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);

		conn.disconnect();
		return results;
	}

}
