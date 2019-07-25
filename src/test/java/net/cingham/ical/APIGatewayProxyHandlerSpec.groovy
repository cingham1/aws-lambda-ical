package net.cingham.ical

import spock.lang.Specification
import spock.lang.Subject

import java.nio.charset.StandardCharsets

import org.apache.commons.lang.StringUtils
import org.apache.commons.io.IOUtils

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import biweekly.property.Summary


class APIGatewayProxyHandlerSpec extends Specification {

    @Subject
    APIGatewayProxyHandler apiGatewayProxyHandler;
	
	ICalHostingRelay iCalRelay
	APIGatewayProxyRequestEvent request
	Context context
	
	private static final String testBody = "test body";
	
    def setup() {
        apiGatewayProxyHandler = new APIGatewayProxyHandler();
		
		iCalRelay = Mock(ICalHostingRelay)
		apiGatewayProxyHandler.iCalRelay = iCalRelay
		
		request = Mock(APIGatewayProxyRequestEvent)
		request.getResource() >> "airbnb"

		context = Mock(Context);
		LambdaLogger logger = Mock(LambdaLogger)
		context.getLogger() >> logger	
    }

    def "test loading valid site data"() {
        given:
			iCalRelay.getICalRelay(_,_) >> testBody
        when:
            APIGatewayProxyResponseEvent response = 
				apiGatewayProxyHandler.handleRequest(request, context)
        then:
			response.getStatusCode() == HttpURLConnection.HTTP_OK
			response.getHeaders().get("Content-Type").equals("text/calendar; charset=utf-8")
			response.getBody().equals(testBody)
    }

	def "test error with invalid type name"() {
		given:
			iCalRelay.getICalRelay(_,_) >> {throw new IllegalArgumentException ("my-error")}
		when:
			APIGatewayProxyResponseEvent response =
				apiGatewayProxyHandler.handleRequest(request, context)
		then:
			response.getStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST
			response.getHeaders().get("Content-Type").equals("application/problem+json")
			response.getBody().contains("message")
			response.getBody().contains("my-error")
	}

	def "test error with internal error"() {
		given:
			iCalRelay.getICalRelay(_,_) >> {throw new IOException ("my-error")}
		when:
			APIGatewayProxyResponseEvent response =
				apiGatewayProxyHandler.handleRequest(request, context)
		then:
			response.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR
			response.getHeaders().get("Content-Type").equals("application/problem+json")
			response.getBody().contains("message")
			response.getBody().contains("my-error")
	}

	def "test health endpoint"() {
		given:
			request.getPath() >> "/health"
		when:
			APIGatewayProxyResponseEvent response =
				apiGatewayProxyHandler.handleRequest(request, context)
		then:
			response.getStatusCode() == HttpURLConnection.HTTP_OK
			response.getHeaders().get("Content-Type").equals("text/plain")
			response.getBody().contains("OK")
	}

}
