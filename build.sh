#!/bin/bash
./gradlew clean jar
cp build/libs/ImageCalc-1.0-SNAPSHOT.jar jar/
cd jar/
mv ImageCalc-1.0-SNAPSHOT.jar ImageCalc.jar
cp /home/maximilian/Programmieren/ImageCalc/build/libs/ImageCalc-1.0-SNAPSHOT.jar /home/maximilian/Programmieren/aster