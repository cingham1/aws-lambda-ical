package net.cingham.ical

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


class ICalHostingRelaySpec extends Specification {

    @Subject
    ICalHostingRelay icalHostingRelay;
	
	String sampleData;
	Context context;
	static final String TYPE_MISTERBANDB = "misterbandb"
	
    def setup() {
        icalHostingRelay = new ICalHostingRelay();		
		sampleData = IOUtils.toString(this.getClass().getResourceAsStream("/ical/sample-mrbnb.ical"), 
			StandardCharsets.UTF_8);
		
		ICalLoader loader = Mock(ICalLoader)
		loader.loadICalData(_) >> sampleData
		ICalRetrievalHandler.icalLoader = loader
		
		context = Mock(Context);
		LambdaLogger logger = Mock(LambdaLogger)
		context.getLogger() >> logger
    }

    def "test loading and filtering 1 site"() {
        given:
        when:
            String result = icalHostingRelay.getICalRelay(TYPE_MISTERBANDB, context)
        then:
			StringUtils.countMatches(result, "Gregory") == 1
			StringUtils.countMatches(result, "BEGIN:VEVENT") == 3
			!result.startsWith("Error")
    }
	
    def "test loading and filtering all sites"() {
        given:
        when:
            String result = icalHostingRelay.getICalRelay(ICalHostingRelay.TYPE_ALL, context)
        then:
			StringUtils.countMatches(result, "Combined Calendars") == 1
			StringUtils.countMatches(result, "Gregory") == 5
			StringUtils.countMatches(result, "BEGIN:VEVENT") > 10
			StringUtils.countMatches(result, "air-") >= 3
			StringUtils.countMatches(result, "hom-") >= 3
			StringUtils.countMatches(result, "mrb-") >= 3
			!result.startsWith("Error")
    }
	
    def "test loading invalid site name"() {
        given:
        when:
            String result = icalHostingRelay.getICalRelay("foo", context)
        then:
			thrown IllegalArgumentException
    }
	
}
