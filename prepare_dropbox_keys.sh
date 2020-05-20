#!/bin/sh
sed -i -b s/"db-q3dzb75sef27mso"/"db-$1"/ app/src/main/AndroidManifest.xml
sed -i -b s/"q3dzb75sef27mso"/"$1"/ app/src/main/java/com/handydev/financisto/export/dropbox/Dropbox.java
echo "Done, don't forget to call git reset --hard after building apk"