cd app\release

del IPTV.apk

rename app-release.apk IPTV.apk

adb connect 192.168.1.102

adb install -r IPTV.apk

cd ..\..\iptv

adb pull /sdcard/Download/iptv/iptv_channels.m3u8 iptv_channels-backup.m3u8
adb push iptv_channels.m3u8 /sdcard/Download/iptv/

cd epg

for %%f in (*.json) do (
    adb pull /sdcard/Download/iptv/epg/%%f backup/
    adb push %%f /sdcard/Download/iptv/epg/
)

cd ..\..\iptv
