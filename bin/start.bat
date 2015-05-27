cd ..
START java -Xmx2G -Xms2G -server -XX:+AggressiveOpts -XX:NewSize=512M -cp "classes;lib/*" org.loklak.LoklakServer >> data/loklak.log 2>&1 & echo $! > data/loklak.pid
echo "loklak server started at port 9100, open your browser at http://localhost:9100"