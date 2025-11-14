@echo off

REM Use this wrapper to download the Gradle wrapper jar if it does not exist
set GRADLE_WRAPPER_JAR=gradle/wrapper/gradle-wrapper.jar

if not exist "%GRADLE_WRAPPER_JAR%" (
  echo Downloading Gradle wrapper jar...
  mkdir gradle\wrapper
  curl -L -o "%GRADLE_WRAPPER_JAR%" https://services.gradle.org/distributions/gradle-6.8.3-bin.zip
)

java -jar "%GRADLE_WRAPPER_JAR%" %*