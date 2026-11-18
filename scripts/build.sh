#!/bin/sh
cd "$(dirname "$0")/.." && mkdir -p bin && \
javac -cp lib/sqlite-jdbc-3.53.4.0.jar -d bin src/Authshield/*.java && echo "Build finished."
