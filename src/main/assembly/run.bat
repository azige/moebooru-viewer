@ehco off
cd /d %~dp0
set file=${project.artifactId}
start javaw -jar lib\%file%.jar
