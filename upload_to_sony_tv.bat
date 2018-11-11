cd app\release

del IPTV.apk

rename app-release.apk IPTV.apk

adb connect 192.168.1.102

adb install -r IPTV.apk

cd ..\..\iptv

adb push iptv_channels.m3u8 /sdcard/Download/iptv/

cd epg

for %%f in (*.json) do (
    adb push %%f /sdcard/Download/iptv/epg/
)

cd ..\..
