#!/bin/sh
cd "$(dirname "$0")/.." && java -cp bin:lib/sqlite-jdbc-3.53.4.0.jar Authshield.SecurityDashboard "$@"
