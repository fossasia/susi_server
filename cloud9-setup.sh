sudo apt-get update
sudo apt-get install -y ant openjdk-8-jdk; git clone https://github.com/fossasia/susi_server.git susi_server
cd susi_server;
sed -i.bak 's/^\(port.http=\).*/\180/'                conf/config.properties
sed -i.bak 's/^\(port.https=\).*/\1443/'              conf/config.properties
sed -i.bak 's/^\(upgradeInterval=\).*/\186400000000/' conf/config.properties
ant
bin/start.sh
