# aws-lambda-ical
ICal Hosting Relay for AWS Lambda

## Overview

When listing a real estate property for Short Term Rental (STR) on multiple travel sites it is necessary to keep booking calendars in sync.  For example, when a guest books a property through AirBnb for Oct 20 - 25, the other booking sites with that property (HomeAway, TripAdvisor, etc) must then show those dates as unavailable to prevent double booking for those dates.

The standard way to keep bookings in sync is through shared calendars using the [ICal](https://en.wikipedia.org/wiki/ICalendar) format.  All booking sites  such as AirBnb, HomeAway, TripAdvisor, MisterBnb etc. provide an ICal feed which can then be linked to other booking sites.

These ICal feeds can also be used by the property manager(s) to see which dates are booked by which guest and from which site they booked on.  These feeds can be imported as standard ICal events into any smartphone/PC calendar.

**Problems:** 

* Multiple ICal feeds are required to be added to a personal calendar (one for each site)
* Each feed formats the information differently, for example:
    * HomeAway prepends each guest name with "Reserved -" 
    * AirBnb adds an unnecessary reservation ID
    * MisterBnb sends duplicate 'Not Available' entries from external booked dates
* It can be hard to distinguish at a glance which site a guest booked from

**Solution:**

Provide a REST API which will normalize the feed data from each site, and optionally retrieve data from all sites in one URL call.  It will also prepend the guest name with a short code to clearly indicate which site the guest booked from (e.g. "air - John Smith" was booked on AirBnb) 


## Architecture

This project is designed to be run as an [AWS Lambda](https://aws.amazon.com/lambda/) function, using a REST API endpoint for data retrieval.  

An ICal client (such as the calendar app on your smartphone) periodically makes requests via a REST API URL for the latest booking data.  The request is routed to the Lambda function which then retrieves the data from the requested hosting site(s) and massages the data according to the rules designated for each site.  The rules are defined in the properties file ical-sites.yml.  The resulting ICal events are collated and returned to the client as an ICal mime-type.

![Architecture Diagram](ical-architecture-diagram.jpg)

#### REST API
The request URL can be for an individual site (such as AirBnb) or all defined sites collated into one response.  The examples below assume DNS (for ical.cingham.net) and API Gateway (for /hosting-relay/{type}) are setup.  The available {type} values are the sites listed in ical-sites.yml, or "all" to gather all of them.

Example URL for HomeAway data only

```
HTTP GET:  https://ical.cingham.net/hosting-relay/homeaway
```
Example URL for all defined sites collated together

```
HTTP GET:  https://ical.cingham.net/hosting-relay/all
```

**Success response:**
* Status: HTTP 200 OK
* Headers: 
    * Content-Type: text/calendar; charset=utf-8
    * Content-Disposition: inline; filename=hosting.ics
* Body: `<data feed text file in ical format>`

**Error response:**
* Status: HTTP 400 Bad Request  or  500 Internal Error
* Headers: 
    * Content-Type: application/problem+json
* Body: `{ "status":<status code>, "message":<error description> }`

## Build & Deploy

This project is built with Java 8 using Maven, and deployed onto Amazon AWS Lambda.

#### Running the tests

Spock unit/integration tests may be run from an IDE or from the command line using the following:

```
mvn test
```

#### Build
Use Mavan to build, run tests, and package for upload to AWS 

```
mvn clean package shade:shade
```

The enclosed shell script *buildit.sh* will do the same thing


#### Deployment to AWS

An AWS account is required.  From the AWS console do the following:

* Create a Lambda function for Java 8 with a name such as "iCalHostingRelay" and upload the built .jar file
* Create an API Gateway for a resource URI such as "/host-api/{type}", 
* In the new API resource URI create a GET method and point it to the Lambda function defined above
* Optionally use Route 53 for a custom domain for the final URL, and point it to the API Gateway url

## Versioning

* 1.0.2 - Added multi-threading to gather all ical sites concurrently
* 1.0.1 - Added regex handling for naming text exclusions; added health URI
* 1.0.0 - Initial version

## Authors

* **Charles Ingham** 

## References 

* [ICal Format Overview](https://en.wikipedia.org/wiki/ICalendar) - wikipedia.org
* [ICal Detailed Spec - RFC 2445](https://www.ietf.org/rfc/rfc2445.txt) - ietf.org

#### Built With

* [Eclipse](https://www.eclipse.org/) - IDE
* [Maven](https://maven.apache.org/) - Dependency Management
* [AWS Lambda](https://aws.amazon.com/lambda/) - Amazon Web Services - Lambda


## License

This project is licensed under the Apache License V2.0 - see the [LICENSE](LICENSE) file for details


