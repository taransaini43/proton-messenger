#!/bin/bash

# Use the environment variable passed from docker-compose.yml
SQLITE_DB_PATH=$SQLITE_DB_PATH

# Run your application with the dynamic configuration
java -jar -Dsqlite.db.path=$SQLITE_DB_PATH /home/app/target/scala-2.13/simple-messenger-rest-assembly-0.1.0-SNAPSHOT.jar