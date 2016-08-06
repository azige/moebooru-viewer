#!/bin/sh

cd `dirname $0`
file=${project.artifactId}
javaw -jar lib/$file.jar
