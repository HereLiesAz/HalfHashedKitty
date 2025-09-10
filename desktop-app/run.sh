#!/bin/bash
<<<<<<< HEAD
<<<<<<< HEAD
java -jar target/connection-manager-1.0-SNAPSHOT-jar-with-dependencies.jar
=======
=======
>>>>>>> origin/feature/build-pc-app
if ! [ -x "$(command -v java)" ]; then
  echo 'Error: java is not installed.' >&2
  exit 1
fi
java -jar target/connection-manager-1.0-SNAPSHOT.jar
<<<<<<< HEAD
>>>>>>> origin/feature/build-pc-app
=======
>>>>>>> origin/feature/build-pc-app
