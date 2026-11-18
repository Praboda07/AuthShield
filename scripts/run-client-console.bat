@echo off
rem Usage: run-client-console.bat [serverIP] [port]
cd /d %~dp0..
java -cp bin;lib\sqlite-jdbc-3.53.4.0.jar Authshield.Authclient %*
