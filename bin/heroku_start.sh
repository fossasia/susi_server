#!/usr/bin/env bash

# Make sure we're on project root
cd $(dirname $0)/..

exec ./bin/start.sh -Idn
