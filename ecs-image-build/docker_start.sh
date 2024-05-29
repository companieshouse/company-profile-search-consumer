#!/bin/bash

PORT=8080

exec java -jar -Dserver.port="${PORT}" "company-profile-search-consumer.jar"