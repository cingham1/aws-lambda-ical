package net.cingham.ical.service;

import biweekly.Biweekly;
import biweekly.ICalVersion;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import net.cingham.ical.domain.SiteInfo;
import net.cingham.ical.domain.SiteMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * Service entry point for tool to gather ICal data from multiple booking sites,
 * and optionally cleanup/normalize the data from each site.
 * 
 * Uses biweekly ICalendar library, see: https://github.com/mangstadt/biweekly
 * 
 * @author cingham
 *
 */
public class ICalHostingRelay {

	public static final String TYPE_ALL = "all";

	private static SiteMap siteMap;

	public ICalHostingRelay() {
		// only load singleton of siteMap data once, for efficiency within aws lambda
		if (siteMap == null) {
			siteMap = new SiteMap();
			siteMap.loadSiteData();
		}
	}

	public String getICalRelay(String type) throws IOException {
		ICalendar icalResults;
		if (StringUtils.equals(type, TYPE_ALL)) {
			icalResults = combineAllCalendars();
		} else {
			SiteInfo siteInfo = siteMap.get(type);
			if (siteInfo == null) {
				throw new IllegalArgumentException("Error - unknown url type passed in: " + type);
			}

			ICalRetrievalHandler handler = new ICalRetrievalHandler(siteInfo);
			icalResults = handler.loadAndMassageSiteData();
		}

		String results = Biweekly.write(icalResults).go();
		return results;
	}

	private ICalendar combineAllCalendars() throws IOException {
		ICalendar combinedIcal = new ICalendar();
		combinedIcal.setVersion(ICalVersion.V2_0);
		List<String> siteNames = new ArrayList<>();

		// create the thread pool to retrieve each site URL concurrently
		ExecutorService service = Executors.newFixedThreadPool(siteMap.size());
		List<Future<ICalendar>> futures = new ArrayList<>();

		try {
			for (SiteInfo site : siteMap.values()) {
				siteNames.add(site.getName());

				// for each site, start the thread to retrieve its data
				ICalRetrievalHandler handler = new ICalRetrievalHandler(site);
				futures.add(service.submit(handler));
			}

			// wait until each thread is done & add events to master list
			for (Future<ICalendar> future : futures) {
				ICalendar ical = future.get(); // blocks until complete
				for (VEvent event : ical.getEvents()) {
					combinedIcal.addEvent(event);
				}
			}
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		} finally {
			service.shutdown();
		}

		combinedIcal.setProductId("//cingham.net//Combined Calendars from " + 
				String.join("-", siteNames) + "//EN");
		return combinedIcal;
	}
}
