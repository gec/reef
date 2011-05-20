set -ex

if [ -z $DEV ]
then
    mvn install -DskipTests -Pdist,preload
else
    mvn install -DskipTests -Pdist
fi

./bin/redeploy.sh

