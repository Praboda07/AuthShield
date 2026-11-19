@echo off
cd /d %~dp0..
if not exist bin mkdir bin
javac -cp lib\sqlite-jdbc-3.53.4.0.jar -d bin src\Authshield\*.java
echo Build finished.
