set -ex

reefdir=reef-karaf-0.3.0-SNAPSHOT-dist

$reefdir/bin/stop > /dev/null 2>&1 || true

rm -rf $reefdir
tar -xvf karaf-linux/target/reef-karaf*.tar.gz
chmod +x $reefdir/bin/*
chmod +x $reefdir/*.sh

