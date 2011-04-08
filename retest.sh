set -ex

mvn install -DskipTests -P karaf

./redeploy.sh

mvn -pl integration -pl api-request -P test test

reef-karaf-*-dist/bin/stop

