#!/bin/bash -x
git submodule update --init
mvn clean install -U -Dmaven.test.skip=true 
