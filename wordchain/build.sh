#!/usr/bin/env bash
# Builds wordchain.jar (needs JDK 17+).
set -e
rm -rf out && mkdir out
javac -d out $(find src -name '*.java')
cp resources/*.txt out/
printf 'Main-Class: wordchain.client.ClientMain\n' > manifest.txt
jar cfm wordchain.jar manifest.txt -C out .
rm manifest.txt
echo "Built wordchain.jar"
