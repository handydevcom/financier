#!/bin/sh
sed -i -b s/"db-YOUR_APP_KEY"/"db-$1"/ app/src/main/AndroidManifest.xml
sed -i -b s/"YOUR_APP_KEY"/"$1"/ app/src/main/java/com/handydev/financisto/export/dropbox/Dropbox.java
echo "Done, don't forget to call git reset --hard after building apk"