#!/usr/bin/env bash

./setup.sh

docker-compose up event-generator-crash
docker-compose up event-generator
docker-compose up -d stream-processor-1
docker-compose up -d stream-aggregator