#!/bin/bash
# 
# Script to run command line tools. Script will include all
# JAR libraries in WEB-INF/lib and all class files in 
# WEB-INF/classes in the classpath.
#
libs=./WEB-INF/lib/*.jar
for lib in $libs; do
  liblist=${liblist}:$lib
done
java -Dgnu.io.rxtx.NoVersionOutput=true  -cp ./WEB-INF/classes:$liblist $@
