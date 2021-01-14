#!/bin/bash

mkdir -p data/zookeeper
echo $ZOOKEEPER_ID > data/zookeeper/myid
bin/pulsar zookeeper
