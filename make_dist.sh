#!/bin/sh

version=$1
commit=`svn info -R | grep ^Revision | sed "s/Revision: \(.*\)/\1/g" | sort -r | head -1`

cp -r www/org.barberaware.GASdotto /tmp/gasdotto-${version}
cd /tmp/gasdotto-${version}

sed "s/\(^.*\$GASDOTTO_VERSION.*=\).*$/\1 \"$version\";/g" server/SystemConf.php -i
sed "s/\(^.*\$GASDOTTO_COMMIT.*=\).*$/\1 \"$commit\";/g" server/SystemConf.php -i
date=`date +"%Y-%m-%d"`
sed "s/\(^.*\$GASDOTTO_BUILT.*=\).*$/\1 \"$date\";/g" server/SystemConf.php -i

cd ..
tar chof - gasdotto-${version} | GZIP=--best gzip -c > gasdotto-${version}.tar.gz
rm -rf gasdotto-${version}
