adb connect 192.168.1.102

adb push iptv_channels.m3u8 /sdcard/Download/iptv/

cd epg

for %%f in (*.json) do (
    adb push %%f /sdcard/Download/iptv/epg/
)

cd ..
