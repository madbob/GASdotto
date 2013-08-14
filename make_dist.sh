#!/bin/sh

version=$1

rm -rf war/WEB-INF/deploy/
rm -rf war/WEB-INF/classes/
rm -rf war/org.barberaware.GASdotto/

ant -f build.xml.prod

cp -r war/org.barberaware.GASdotto /tmp/gasdotto-${version}
cd /tmp/gasdotto-${version}

sed "s/\(^.*\$GASDOTTO_VERSION.*=\).*$/\1 \"$version\";/g" server/SystemConf.php -i
date=`date +"%Y-%m-%d"`
sed "s/\(^.*\$GASDOTTO_BUILT.*=\).*$/\1 \"$date\";/g" server/SystemConf.php -i

cd ..
tar chof - gasdotto-${version} | GZIP=--best gzip -c > gasdotto-${version}.tar.gz
rm -rf gasdotto-${version}
