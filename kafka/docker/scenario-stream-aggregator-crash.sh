#!/usr/bin/env bash

./setup.sh

docker-compose up event-generator
docker-compose up -d stream-processor-1 stream-processor-2
docker-compose up stream-aggregator-crash
docker-compose up -d stream-aggregator