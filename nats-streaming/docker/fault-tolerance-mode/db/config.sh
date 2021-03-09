#!/bin/bash
psql -U postgres --command "ALTER USER postgres WITH SUPERUSER PASSWORD 'postgres';"
