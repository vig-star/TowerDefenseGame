#!/usr/bin/env sh
# Compile, exercise the real JavaFX UI handlers, and snapshot the game scene.
set -eu

snapshot_project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$snapshot_project_root"

./mvnw --batch-mode --no-transfer-progress -DskipTests package \
  org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath \
  -DincludeScope=runtime -Dmdep.outputFile=target/runtime-classpath.txt

snapshot_classpath="$snapshot_project_root/target/classes:$(cat target/runtime-classpath.txt)"
snapshot_java=java
snapshot_javac=javac
if [ -n "${JAVA_HOME:-}" ]; then
  snapshot_java="$JAVA_HOME/bin/java"
  snapshot_javac="$JAVA_HOME/bin/javac"
fi

mkdir -p target/smoke
"$snapshot_javac" --release 11 -cp "$snapshot_classpath" -d target/smoke \
  scripts/TowerDefenseSmokeSnapshot.java
"$snapshot_java" -cp "$snapshot_project_root/target/smoke:$snapshot_classpath" \
  TowerDefenseSmokeSnapshot "${1:-$snapshot_project_root/docs/images/gameplay.png}"
