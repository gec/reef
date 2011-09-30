set -ex

#./bin/deploy.sh

mvn install -DskipTests -Pdist

./bin/redeploy-integration-tests.sh

mvn -pl integration -pl api-request -P test test

if [ $? -ne 0 ]; then
    echo "maven tests failed"
    exit 2
fi

totalgrid-reef-*/bin/stop
