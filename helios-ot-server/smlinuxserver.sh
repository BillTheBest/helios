#!/bin/bash
############################################
#  Helios OT Server Launcher
#  Whitehead
#  Jan 1, 2012
#  Notes
#  =====
#  	The eclipse launcher shortcut requires that the helios-ot-server eclipse project be built using "mvn eclipse:eclipse"
############################################
export M2_REPO=~/.m2/repository
#export JAVA_HOME=/usr/lib/jvm/java-6-sun
export EXE_JAR=$M2_REPO/org/helios/helios-spring/helios-spring-launcher/1.0-SNAPSHOT/helios-spring-launcher-1.0-SNAPSHOT.jar
export CMD_LINE="-el ./src/test/resources/server/conf/smlinuxserver.launch -conf ./src/test/resources/server/conf -log4j ./src/test/resources/server/conf/log4j/log4j.xml -daemon "
#export CMD_LINE="$CMD_LINE -lib $M2_REPO/org/slf4j/slf4j-log4j12/1.5.8 "
export CMD_LINE="$CMD_LINE -lib $M2_REPO/org/helios/helios-ot-server/1.0-SNAPSHOT "
export CMD_LINE="$CMD_LINE -lib $M2_REPO/org/helios/helios-collectors/helios-collectors-core/1.0-SNAPSHOT "
export CMD_LINE="$CMD_LINE -lib $M2_REPO/org/helios/helios-scripting/1.0-SNAPSHOT "
export CMD_LINE="$CMD_LINE -lib $M2_REPO/org/helios/helios-ot/helios-ot-core2/1.0-SNAPSHOT "
#export SYS_PROPS="-Dactivemq.base=/tmp/activemq"
export SYS_PROPS=" -Djava.rmi.server.hostname=helios-ubuntu"
export JAVA_OPTS=""

echo "============================================================================"
echo ""
echo "$JAVA_HOME/bin/java $JAVA_OPTS $SYS_PROPS -jar $EXE_JAR $CMD_LINE"
echo ""
echo "============================================================================"
$JAVA_HOME/bin/java $JAVA_OPTS $SYS_PROPS -jar $EXE_JAR $CMD_LINE

