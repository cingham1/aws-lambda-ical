package net.cingham.ical;


/**
 * 
 * Entity to hold info for each ICal site, and how to massage the gathered data 
 *
 * @author cingham
 * 
 */
public class SiteInfo {	
	private String name;
	private String url;
	private String removeRegex;
	private String addPrefix;
	private String excludeEvents;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getRemoveRegex() {
		return removeRegex;
	}
	public void setRemoveRegex(String removeRegex) {
		this.removeRegex = removeRegex;
	}
	public String getAddPrefix() {
		return addPrefix;
	}
	public void setAddPrefix(String addPrefix) {
		this.addPrefix = addPrefix;
	}
	public String getExcludeEvents() {
		return excludeEvents;
	}
	public void setExcludeEvents(String excludeEvents) {
		this.excludeEvents = excludeEvents;
	}
}
