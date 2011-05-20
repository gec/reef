set -ex

mvn install -DskipTests -Pdist

./dev_install.sh

mvn -pl integration -pl api-request -P test test

totalgrid-reef-*/bin/stop

