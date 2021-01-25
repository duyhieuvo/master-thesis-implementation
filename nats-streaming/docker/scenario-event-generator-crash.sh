#!/usr/bin/env bash

./setup.sh

docker-compose up event-generator-crash
docker-compose up event-generator
docker-compose up stream-processor
docker-compose up stream-aggregator