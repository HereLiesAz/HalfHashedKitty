#!/bin/bash
<<<<<<< HEAD
java -jar target/connection-manager-1.0-SNAPSHOT-jar-with-dependencies.jar
=======
if ! [ -x "$(command -v java)" ]; then
  echo 'Error: java is not installed.' >&2
  exit 1
fi
java -jar target/connection-manager-1.0-SNAPSHOT.jar
>>>>>>> origin/feature/build-pc-app
