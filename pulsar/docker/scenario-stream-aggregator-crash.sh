#!/usr/bin/env bash

./setup.sh

docker-compose up event-generator
docker-compose up stream-processor-1
docker-compose up stream-aggregator-crash
docker-compose up stream-aggregator