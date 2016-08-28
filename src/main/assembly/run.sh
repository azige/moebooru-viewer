#!/bin/sh

cd `dirname $0`
file=${project.artifactId}
java -jar lib/$file.jar
