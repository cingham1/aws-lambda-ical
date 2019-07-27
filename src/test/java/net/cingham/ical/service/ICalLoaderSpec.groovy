package net.cingham.ical.service

import spock.lang.Specification
import spock.lang.Subject

import java.nio.charset.StandardCharsets

import org.apache.commons.lang.StringUtils
import org.apache.commons.io.IOUtils

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import biweekly.property.Summary
import net.cingham.ical.domain.SiteInfo
import net.cingham.ical.domain.SiteMap
import net.cingham.ical.service.ICalLoader


class ICalLoaderSpec extends Specification {

	@Subject
	private ICalLoader iCalLoader

	static final String TYPE_MISTERBANDB = "misterbandb"

	def setup() {
		iCalLoader = new ICalLoader();
	}

	def "integration test loading a live site"() {
		given:
			SiteMap siteMap = new SiteMap();
			siteMap.loadSiteData();
			SiteInfo siteInfo = siteMap.get(TYPE_MISTERBANDB);
		when:
			String result = iCalLoader.loadICalData(siteInfo)
		then:
			StringUtils.countMatches(result, "PRODID:-//Misterb&b") == 1
			StringUtils.countMatches(result, "BEGIN:VEVENT") > 2
	}

	def "integration test trying to load invalid url"() {
		given:
			SiteInfo siteInfo = new SiteInfo()
			siteInfo.setUrl("foobar")
		when:
			String result = iCalLoader.loadICalData(siteInfo)
		then:
			thrown IOException
	}
}
