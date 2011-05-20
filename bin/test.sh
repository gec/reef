set -ex

./bin/deploy.sh

mvn -pl integration -pl api-request -P test test

totalgrid-reef-*/bin/stop