:: ############################################
:: #  Helios OT Server Launcher
:: #  Whitehead
:: #  Jan 1, 2012
:: #  Notes
:: #  =====
:: #  	The eclipse launcher shortcut requires that the helios-ot-server eclipse project be built using mvn eclipse:eclipse
:: ############################################
@echo off
cls
set M2_REPO=%HOME%\.m2\repository
set EXE_JAR=%M2_REPO%\org\helios\helios-spring\helios-spring-launcher\1.0-SNAPSHOT\helios-spring-launcher-1.0-SNAPSHOT.jar
set CMD_LINE=-el .\src\test\resources\server\conf\OTServer.launch -conf .\src\test\resources\server\conf -log4j .\src\test\resources\server\conf\log4j\log4j.xml -daemon 
set CMD_LINE=%CMD_LINE% -lib %M2_REPO%\org\helios\helios-ot-server\1.0-SNAPSHOT 
set CMD_LINE=%CMD_LINE% -lib %M2_REPO%\org\helios\helios-collectors\helios-collectors-core\1.0-SNAPSHOT 
set CMD_LINE=%CMD_LINE% -lib %M2_REPO%\org\helios\helios-scripting\1.0-SNAPSHOT 
set CMD_LINE=%CMD_LINE% -lib %M2_REPO%\org\helios\helios-ot\helios-ot-core2\1.0-SNAPSHOT 
set SYS_PROPS=
set JAVA_OPTS=

echo ============================================================================
echo 
echo %JAVA_HOME%\bin\java %JAVA_OPTS% %SYS_PROPS% -jar %EXE_JAR% %CMD_LINE%
echo 
echo ============================================================================
%JAVA_HOME%\bin\java %JAVA_OPTS% %SYS_PROPS% -jar %EXE_JAR% %CMD_LINE%

