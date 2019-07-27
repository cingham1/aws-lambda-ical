package net.cingham.ical.domain;

import java.io.InputStream;
import java.util.HashMap;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import biweekly.util.IOUtils;

/***
 * 
 * Map object used to load the site data from a config .yml file. 
 * For each site:  key=site name, value=full SiteInfo
 * 
 * Uses Snakeyaml library for loading, see: https://www.baeldung.com/java-snake-yaml
 * 
 * @author cingham
 * 
 */
public class SiteMap extends HashMap<String, SiteInfo> {
	static final long serialVersionUID = 1l;

	public SiteMap() {
	}

	public void loadSiteData() {
		Yaml yaml = new Yaml(new Constructor(SiteInfo.class));
		InputStream inputStream = this.getClass().getResourceAsStream("/ical-sites.yml");
		for (Object obj : yaml.loadAll(inputStream)) {
			SiteInfo info = (SiteInfo)obj;
			this.put(info.getName(), info);
		}
		IOUtils.closeQuietly(inputStream);
	}
}
