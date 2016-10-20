cd ..
START java -Xmx4G -Xms1G -server -XX:+AggressiveOpts -XX:NewSize=512M -cp "classes;lib/*" org.loklak.SusiServer >> data/loklak.log 2>&1 & echo $! > data/loklak.pid
echo "susi server started at port 4000, open your browser at http://localhost:4000"