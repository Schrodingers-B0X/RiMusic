del app\release\app-release.apks
java -jar ./bundletool.jar build-apks --ks="./APK keystore/RiMusicKeystore.jks" --ks-key-alias=key0 --bundle=./app/release/app-release.aab --output=./app/release/app-release.apks