#!/usr/bin/env bash

./setup.sh

docker-compose up event-generator
docker-compose up stream-processor-2-zombie-instance stream-processor-1
docker-compose up -d stream-aggregator