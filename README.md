# `company-profile-search-consumer`

## Summary

The ``company-profile-search-consumer`` handles the processing of company profile sub-deltas by:

* Listening of the `resource-changed` Kafka topic, consume messages in the forms of `ResourceChangedData` Kafka messages, 
* deserialising them and transforming them into a structure suitable for a request to `search.api.ch.gov.uk`, and
* sending the request internally while performing any error handling.

In conjunction with [search.api.ch.gov.uk](https://github.com/companieshouse/search.api.ch.gov.uk) the services are responsible for managing company search indices in the Elasticsearch primary index.
The service is implemented in Java 21 using Spring Boot 3.2

## Error handling

The table below describes the topic a Kafka message is published to when an API error response is received, given the
number of attempts to process that message. The number of attempts is incremented when processed from the main or
retry topic. Any runtime exceptions thrown during the processing of a message are handled by publishing the message
immediately to the <br>`stream-company-profile-company-profile-search-consumer-invalid` topic and are not retried.

| API Response | Attempt          | Topic published to                                             |
|--------------|------------------|----------------------------------------------------------------|
| 2xx          | any              | _does not republish_                                           |
| 4xx          | any              | stream-company-profile-company-profile-search-consumer-invalid |
| 5xx          | < max_attempts   | stream-company-profile-company-profile-search-consumer-retry   |
| 5xx          | \>= max_attempts | stream-company-profile-company-profile-search-consumer-error   |

## System requirements

* [Git](https://git-scm.com/downloads)
* [Java](http://www.oracle.com/technetwork/java/javase/downloads)
* [Maven](https://maven.apache.org/download.cgi)
* [Apache Kafka](https://kafka.apache.org/)

## Building and Running Locally using Docker

1. Clone [Docker CHS Development](https://github.com/companieshouse/docker-chs-development) and follow the steps in the
   README.
2. Enable the following services using the command `./bin/chs-dev services enable <service>`.
    * `chs-delta-api`
    * `company-profile-delta-consumer`
    * `company-profile-api`
    * `company-profile-search-consumer`
    * `search.api.ch.gov.uk`
3. Boot up the services' containers on docker using tilt `tilt up`.
4. Being a sub-delta, messages can be produced to the `stream-company-profile` topic by posting a company profile delta (see instructions in [CHS Delta API](https://github.com/companieshouse/chs-delta-api).)
5. Alternatively, sending valid resource-changed messages is a way to trigger sub-deltas independently. 

## Environment variables

| Variable                                    | Description                                                                         | Example (from docker-chs-development) |
|---------------------------------------------|-------------------------------------------------------------------------------------|---------------------------------------|
| PORT                                        | The server port of this service                                                     | 8081                                  |
| DATA_SYNC_KAFKA_BROKER_URL                  | The URL to the kafka broker                                                         | kafka:9092                            |
| COMPANY_PROFILE_SEARCH_LISTENER_CONCURRENCY | The number of listeners run in parallel for the consumer                            | 1                                     |
| COMPANY_PROFILE_SEARCH_GROUP_ID             | The group ID for the service's Kafka topics                                         | company-profile-search-consumer       |
| COMPANY_PROFILE_SEARCH_BACKOFF_DELAY        | The incremental time delay between message retries                                  | stream-company-profile                |
| COMPANY_PROFILE_SEARCH_ATTEMPTS             | The number of times a message will be retried before being moved to the error topic | 5                                     |
| STREAM_COMPANY_PROFILE_TOPIC                | The topic ID for the  stream-company-profile kafka topic                            | 100                                   |

## Building the docker image
```bash
mvn package -Dskip.unit.tests=true -Dskip.integration.tests=true jib:dockerBuild
```

## To make local changes

Development mode is available for this service
in [Docker CHS Development](https://github.com/companieshouse/docker-chs-development).

    ./bin/chs-dev development enable company-profile-search-consumer

This will clone the `company-profile-search-consumer` into the repositories folder. Any changes to the code, or resources
will automatically trigger a rebuild and relaunch.