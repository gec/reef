set -ex

reefdir=reef-karaf-0.1.2-SNAPSHOT-dist

$reefdir/bin/stop > /dev/null 2>&1 || true

mvn install -DskipTests -P karaf

rm -rf $reefdir
tar -xvf karaf-linux/target/reef-karaf*.tar.gz
chmod +x $reefdir/bin/*
chmod +x $reefdir/*.sh

current=`pwd`

cd $reefdir

./quick_start.sh

cd $current

mvn -pl integration -P test test

$reefdir/bin/stop

