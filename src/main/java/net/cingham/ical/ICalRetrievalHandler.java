package net.cingham.ical;

import java.io.IOException;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.Summary;


/**
 * 
 * Load and massage the ICal data from a particular booking site.
 * Defined as a Callable thread so multiple sites can be gathered concurrently.
 * 
 * @author cingham
 *
 */
public class ICalRetrievalHandler implements Callable<ICalendar> {
	
	private static ICalLoader icalLoader = new ICalLoader();
	
	SiteInfo siteInfo;
	
	ICalRetrievalHandler(SiteInfo siteInfo) {
		this.siteInfo = siteInfo;
	}
	
	// optionally run in a thread
	public ICalendar call() throws IOException {
		return loadAndMassageSiteData();
	}
		
	public ICalendar loadAndMassageSiteData() throws IOException {
		// load live iCal data
		String icalDataString = icalLoader.loadICalData(siteInfo);
    	ICalendar ical = Biweekly.parse(icalDataString).first();
    
        // massage event data as necessary
       	filterExcludeEvents(ical, siteInfo.getExcludeEvents());	  
       	removeRegexFromEvents(ical, siteInfo.getRemoveRegex());
        addPrefixToEvents(ical, siteInfo.getAddPrefix());
        	
        return ical;
	}
	
	/**
	 * Special handling for some sites (e.g. misterbandb) to remove extra 'not available' 
	 * events added in from other bookings.
	 */
	private void filterExcludeEvents(ICalendar ical, String excludeEvents) {
        if (StringUtils.isEmpty(excludeEvents)) {
        	return;
        }

		ListIterator<VEvent> iter = ical.getEvents().listIterator();
        while(iter.hasNext()){
        	VEvent event = iter.next();
        	Summary summary = event.getSummary();
        	if (summary != null && summary.getValue().equalsIgnoreCase(excludeEvents)) {
        		iter.remove();
        	}
        }
	}	

	/**
	 * Optionally remove string patterns from events (e.g. remove "Reserved -" from "Reserved - Jeff Jones")
	 */
	private void removeRegexFromEvents(ICalendar ical, String regex) {
        if (StringUtils.isEmpty(regex)) {
        	return;
        }
        Pattern pattern = Pattern.compile(regex);
        
		ListIterator<VEvent> iter = ical.getEvents().listIterator();
        while(iter.hasNext()){
        	VEvent event = iter.next();
        	Summary summary = event.getSummary();
        	if (summary != null) {
        		String value = summary.getValue();
        		
        		String newValue = pattern.matcher(value).replaceAll("");
        		event.setSummary(newValue);
        	}
        }
	}	

	/**
	 * Optionally add prefix to event summary (e.g. "air - Jeff Jones")
	 */
	private void addPrefixToEvents(ICalendar ical, String prefix) {
        if (StringUtils.isEmpty(prefix)) {
        	return;
        }

		ListIterator<VEvent> iter = ical.getEvents().listIterator();
        while(iter.hasNext()){
        	VEvent event = iter.next();
        	Summary summary = event.getSummary();
        	if (summary != null) {
        		event.setSummary(prefix + "- " + summary.getValue());
        	}
        }
	}	

}
