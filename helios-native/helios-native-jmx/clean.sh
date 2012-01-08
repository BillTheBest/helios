#!/bin/bash
JJ=/home/nwhitehead/libs/java/JarJar/jarjar-1.1.jar
java -jar $JJ process rules.txt ./target/helios-native-jmx-launcher.jar ./helios-nagent.jar
echo "Generated helios-nagent.jar"
