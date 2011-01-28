set -ex

apache-karaf-2.1.3/bin/stop || true

mvn install -DskipTests -P karaf

rm -rf apache-karaf-2.1.3
tar -xvf karaf-linux/target/reef-karaf*.tar.gz
chmod +x apache-karaf-2.1.3/bin/*
chmod +x apache-karaf-2.1.3/*.sh

current=`pwd`

cd apache-karaf-2.1.3

./quick_start.sh

cd $current

mvn -pl integration -P test test



