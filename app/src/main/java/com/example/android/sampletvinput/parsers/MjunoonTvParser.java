package com.example.android.sampletvinput.parsers;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;
import com.google.android.exoplayer.C;
import com.google.android.exoplayer.hls.HlsMasterPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class MjunoonTvParser extends VideoUrlParser {
    private static final String TAG = "MjnunoonTvParser";
    private static final String PARSER_ID = "mjunoon.tv";

    private static final String TAG_SEC = "sec=";
    private static final String TAG_WMS_AUTH_SIGN = "?wmsAuthSign=";

    private static final String TAG_STREAM_URL = "streamUrl=";
    private static final String TAG_BACKUP_STREAM_URL = "backup_live_stream_url=";
    private static final String TAG_TEMP_URL = "temp_url=";

    @Override
    public String getParserId() {
        return PARSER_ID;
    }

    @Override
    public String parseVideoUrl(String videoUrl){
        String html_response = null;

        try {
            html_response = SimpleHttpClient.GET(videoUrl, Util.USER_AGENT_FIREFOX);
            html_response = Util.Html.getTag(html_response, "<script id=\"playerScript\"", "</script>");

            videoUrl = extractStreamUrl(html_response);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return videoUrl;
    }

    private static String extractStreamUrl(String data) throws IOException
    {
        String wmsAuthSign = Util.Html.getTag(data, TAG_SEC, "\"");

        String[] streamUrls = {
                Util.Html.getTag(data, TAG_STREAM_URL, "&") + TAG_WMS_AUTH_SIGN + wmsAuthSign,
                Util.Html.getTag(data, TAG_TEMP_URL, "&") + TAG_WMS_AUTH_SIGN + wmsAuthSign,
        Util.Html.getTag(data, TAG_BACKUP_STREAM_URL, "&") + TAG_WMS_AUTH_SIGN + wmsAuthSign };

        for (String streamUrl : streamUrls) {
            String html_response = SimpleHttpClient.GET(streamUrl, Util.USER_AGENT_FIREFOX);

            if(html_response != null && html_response.contains("#EXTM3U")) {
                return parseHlsManifest(streamUrl, html_response);
            }
        }

        return null;
    }

    private static String parseHlsManifest(String streamUrl, String html_response) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(
                html_response.getBytes(Charset.forName(C.UTF8_NAME)));

        HlsMasterPlaylist playlist = (HlsMasterPlaylist) new HlsPlaylistParser().parse(streamUrl, inputStream);

        if(playlist.variants.size() > 0) {
            int maxBitRate = playlist.variants.get(0).format.bitrate;
            String videoUrl = playlist.variants.get(0).url;

            // select the variant with max bit-rate
            for (int i=1; i < playlist.variants.size(); i++) {
                if(playlist.variants.get(i).format.bitrate > maxBitRate) {
                    maxBitRate = playlist.variants.get(i).format.bitrate;
                    videoUrl = playlist.variants.get(i).url;
                }
            }

            if(videoUrl.startsWith("http")) {
                return videoUrl;
            } else {
                return streamUrl;
            }
        }

        return null;
    }
}
