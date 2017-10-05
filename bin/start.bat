cd ..
START java -Xmx4G -Xms1G -server -XX:+AggressiveOpts -XX:NewSize=512M -cp "classes;lib/*" ai.susi.SusiServer >> data/susi.log 2>&1 & echo $! > data/susi.pid
echo "susi server started at port 4000, open your browser at http://localhost:4000"