@echo off
REM Builds wordchain.jar (needs JDK 17+ on PATH).
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt || exit /b 1
del sources.txt
copy resources\*.txt out\ >nul
echo Main-Class: wordchain.client.ClientMain> manifest.txt
jar cfm wordchain.jar manifest.txt -C out .
del manifest.txt
echo Built wordchain.jar
