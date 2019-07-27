package net.cingham.ical.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * Entity to hold info for each ICal site, and how to massage the gathered data
 *
 * @author cingham
 * 
 */
@Getter
@Setter
public class SiteInfo {
	
	private String name;
	private String url;
	private String removeRegex;
	private String addPrefix;
	private String excludeEvents;
	
}
