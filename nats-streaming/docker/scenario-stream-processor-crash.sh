#!/usr/bin/env bash

./setup.sh

docker-compose up event-generator
docker-compose up stream-processor-crash
docker-compose up -d stream-processor
docker-compose up -d stream-aggregator