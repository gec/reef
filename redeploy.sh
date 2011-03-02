set -ex

reefdir=reef-karaf-0.2.2-dist

$reefdir/bin/stop > /dev/null 2>&1 || true

rm -rf $reefdir
tar -xvf karaf-linux/target/reef-karaf*.tar.gz
chmod +x $reefdir/bin/*
chmod +x $reefdir/*.sh

current=`pwd`

cd $reefdir

./quick_start.sh

cd $current

