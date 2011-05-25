set -ex

mvn clean install -P karaf
if [ $? -ne 0 ]; then
    echo "maven build failed"
    exit 1
fi

./redeploy.sh

mvn -pl integration -pl api-request -P test test
if [ $? -ne 0 ]; then
    echo "maven tests failed"
    exit 2
fi

reef-karaf-*-dist/bin/stop

