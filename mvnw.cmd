@REM Maven Wrapper batch file for Windows
@REM Downloads Maven 3.9.9 on first run.
@echo off
setlocal

set MAVEN_WRAPPER_JAR=.mvn\wrapper\maven-wrapper.jar
set MAVEN_WRAPPER_PROPERTIES=.mvn\wrapper\maven-wrapper.properties

for /f "tokens=2 delims==" %%i in ('findstr /b "wrapperUrl=" "%MAVEN_WRAPPER_PROPERTIES%"') do set WRAPPER_JAR_URL=%%i

if not exist "%MAVEN_WRAPPER_JAR%" (
    if exist "%JAVA_HOME%\bin\java.exe" (
        "%JAVA_HOME%\bin\java.exe" -classpath "" org.apache.maven.wrapper.BootstrapMainStarter "%WRAPPER_JAR_URL%" "%MAVEN_WRAPPER_JAR%" 2>nul
    )
    if not exist "%MAVEN_WRAPPER_JAR%" (
        echo Downloading maven-wrapper.jar...
        powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_JAR_URL%' -OutFile '%MAVEN_WRAPPER_JAR%'" 2>nul
    )
)

if not exist "%MAVEN_WRAPPER_JAR%" (
    echo ERROR: Could not download maven-wrapper.jar
    exit /b 1
)

set REPO=
set MAVEN_CMD_LINE_ARGS=%*

"%JAVA_HOME%\bin\java.exe" %MAVEN_OPTS% ^
  -classpath "%MAVEN_WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%CD%" ^
  "org.apache.maven.wrapper.MavenWrapperMain" ^
  %MAVEN_CMD_LINE_ARGS%
