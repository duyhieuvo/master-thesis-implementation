#!/bin/bash
createdb -U postgres -E UTF8 exactly-once
psql -U postgres --command "ALTER USER postgres WITH SUPERUSER PASSWORD 'postgres';"
