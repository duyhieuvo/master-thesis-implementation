#!/usr/bin/env bash

./setup.sh

docker-compose up -d event-generator
docker-compose up -d stream-processor-1
docker-compose up -d stream-aggregator