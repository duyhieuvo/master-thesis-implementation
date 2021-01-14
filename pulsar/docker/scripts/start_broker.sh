#!/bin/bash
cp /custom-conf/functions_worker.yml /pulsar/conf/functions_worker.yml
bin/pulsar broker
