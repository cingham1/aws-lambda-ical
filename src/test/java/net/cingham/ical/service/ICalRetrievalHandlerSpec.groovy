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
import net.cingham.ical.service.ICalLoader
import net.cingham.ical.service.ICalRetrievalHandler


class ICalRetrievalHandlerSpec extends Specification {

	@Subject
	ICalRetrievalHandler iCalRetrievalHandler

	SiteInfo siteInfo
	String sampleData
	ICalendar originalICal


	def setup() {
		siteInfo = new SiteInfo()
		siteInfo.setName("misterbandb")
		siteInfo.setUrl("dummy-url")
		siteInfo.setAddPrefix("mrb:")
		siteInfo.setExcludeEvents("Not available")

		iCalRetrievalHandler = new ICalRetrievalHandler(siteInfo)

		sampleData = IOUtils.toString(this.getClass().getResourceAsStream("/ical/sample-mrbnb.ical"),
				StandardCharsets.UTF_8)

		ICalLoader loader = Mock(ICalLoader)
		loader.loadICalData(_) >> sampleData
		ICalRetrievalHandler.icalLoader = loader

		originalICal = Biweekly.parse(sampleData).first()
	}

	def "test loading and filtering 1 site"() {
		given:
		when:
			ICalendar iCal = iCalRetrievalHandler.loadAndMassageSiteData()
		then:
			originalICal.getEvents().size() == 7
			iCal.getEvents().size() == 3
	}

	def "test filterExcludeEvents"() {
		given:
			ICalendar iCal = Biweekly.parse(sampleData).first();
		when:
			iCalRetrievalHandler.filterExcludeEvents(iCal, "not available")
		then:
			originalICal.getEvents().size() == 7
			containsExcludeStringEvent(originalICal, "not available") == true
			iCal.getEvents().size() == 3
			containsExcludeStringEvent(iCal, "not available") == false
	}

	def "test removeRegexFromEvents removing 'Reserved -'"() {
		given:
			ICalendar iCal = Biweekly.parse(sampleData).first();
		when:
			iCalRetrievalHandler.removeRegexFromEvents(iCal, "Reserved -")
		then:
			containsPrefix(originalICal, "Reserved -") == true
			containsPrefix(iCal, "Reserved -") == false
	}

	def "test removeRegexFromEvents removing '(nnn)'"() {
		given:
			ICalendar iCal = Biweekly.parse(sampleData).first();
		when:
			iCalRetrievalHandler.removeRegexFromEvents(iCal, "\\s*\\([^\\)]*\\)\\s*")
		then:
			containsSummaryText(originalICal, "(HMCTAMYBE3)") == true
			containsSummaryText(iCal, "(HMCTAMYBE3)") == false
	}

	def "test addPrefixToEvents"() {
		given:
			ICalendar iCal = Biweekly.parse(sampleData).first();
		when:
			iCalRetrievalHandler.addPrefixToEvents(iCal, "air")
		then:
			containsPrefix(originalICal, "air- ") == false
			containsPrefix(iCal, "air- ") == true
	}

	private boolean containsExcludeStringEvent(ICalendar iCal, String excludeString) {
		for (VEvent event : iCal.getEvents()) {
			Summary summary = event.getSummary();
			if (summary != null && summary.getValue().equalsIgnoreCase(excludeString)) {
				return true
			}
		}
		return false;
	}

	private boolean containsPrefix(ICalendar iCal, String prefix) {
		for (VEvent event : iCal.getEvents()) {
			Summary summary = event.getSummary();
			if (summary != null && summary.getValue().startsWith(prefix)) {
				return true
			}
		}
		return false;
	}

	private boolean containsSummaryText(ICalendar iCal, String summaryText) {
		for (VEvent event : iCal.getEvents()) {
			Summary summary = event.getSummary();
			if (summary != null && summary.getValue().contains(summaryText)) {
				return true
			}
		}
		return false;
	}
}
