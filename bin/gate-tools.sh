#!/bin/bash 

# Script provides various gate related utilities. 


PRG=$0
#DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
DEBUG=

# resolve symlinks
while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '^.*-> \(.*\)$' 2>/dev/null`
    if expr "$link" : '^/' 2> /dev/null >/dev/null; then
	PRG="$link"
    else
	PRG="`dirname "$PRG"`/$link"
    fi
done
PROJECT_DIR=`dirname "$PRG"`
JAR_FILE="$PROJECT_DIR"/../target/gate-tools-*-jar-with-dependencies.jar


if [ ! -f $JAR_FILE ]; then 
	cd ..
	mvn clean package	
	cd - 
fi

java $DEBUG -jar $JAR_FILE "$@"
