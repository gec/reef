set -ex

mvn install -DskipTests -Pdist

./bin/redeploy.sh

