#!/usr/bin/env bash

./setup.sh

docker-compose up event-generator-crash
docker-compose up event-generator
docker-compose up -d stream-processor
docker-compose up -d stream-aggregator