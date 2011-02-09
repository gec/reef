set -ex

mvn install -DskipTests -P karaf

./redeploy.sh

mvn -pl integration -P test test

reef-karaf-*-dist/bin/stop
