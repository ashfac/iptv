package com.example.android.sampletvinput.parsers;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents youtube video information retriever.
 */
public class YouTubeParser extends VideoUrlParser
{
    private static final String TAG = "YouTubeParser";
    private static final String PARSER_ID = "youtube";
    private static final String URL_TYPE_CHANNEL = "youtube.com/channel/";
    private static final String URL_TYPE_EMBED_CHANNEL = "embed/live_stream?channel";
    private static final String URL_TYPE_EMBED = "/youtube-embed";

    private static final String URL_YOUTUBE_GET_VIDEO_INFO = "https://www.youtube.com/get_video_info?&video_id=";
    private static final String URL_YOUTUBE_GET_VIDEO_INFO_PARAMS = "&gl=US&hl=en&ps=default&eurl=https://youtube.googleapis.com/v/";

    public static final String KEY_LIVE_STREAM = "live_playback";
    public static final String KEY_PLAYER_RESPONSE = "player_response";

    public static final String KEY_STREAMING_DATA = "streamingData";
    public static final String KEY_HLS_MANIFEST_URL = "hlsManifestUrl";

    private static final String KEY_SECURE_URL = "<meta property=\"og:video:secure_url\" content=\"";
    private static final String KEY_EMBED_TO_VIDEO_ID = "youtube.com/embed/";
    private static final String KEY_EMBED_CHANNEL_TO_VIDEO_ID = "youtube.com/watch?v=";

    private TreeMap<String, String> kvpList = new TreeMap<>();

    @Override
    public String getParserId() {
        return PARSER_ID;
    }

    @Override
    public String parseVideoUrl(String videoUrl){
        if (videoUrl.contains(URL_TYPE_CHANNEL)) {
            return parseChannelUrl(videoUrl);
        } else if (videoUrl.contains(URL_TYPE_EMBED_CHANNEL)) {
            return parseEmbedChannelUrl(videoUrl);
        } else if (videoUrl.contains(URL_TYPE_EMBED)) {
            return parseEmbedUrl(videoUrl);
        } else {
            return videoIdToVideoUrl(videoUrl.substring(videoUrl.indexOf("=")+1));
        }
    }

    public String parseChannelUrl(String videoUrl) {
        try {
            String html_response = SimpleHttpClient.GET(videoUrl, Util.USER_AGENT_FIREFOX);
            String videoId = Util.Html.getTag(html_response, KEY_SECURE_URL, "\"");

            if (videoId != null) {
                if (videoId.contains("/embed/")) {
                    return parseEmbedChannelUrl(videoId);
                }
                else if (videoId.contains("/v/")) {
                    videoId = videoId.substring(0, videoId.indexOf("/v/"));
                    if (videoId.contains("?")) {
                        videoId = videoId.substring(0, videoId.indexOf("?"));
                    }
                }
            }

            return videoIdToVideoUrl(videoId);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return null;
    }

    public String parseEmbedChannelUrl(String videoUrl) {
        return videoIdToVideoUrl(embedChannelToVideoId(videoUrl));
    }

    public String embedChannelToVideoId(String videoUrl) {
        try {
            String html_response = SimpleHttpClient.GET(videoUrl);
            String videoId = Util.Html.getTag(html_response, KEY_EMBED_CHANNEL_TO_VIDEO_ID, "\"");

            if (videoId != null && videoId.contains("?")) {
                videoId = videoId.substring(0, videoId.indexOf("?"));
            }

            return videoId;

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return null;
    }

    public String parseEmbedUrl(String videoUrl) {
        try {
            // remove the extra /youtube-embed part
            videoUrl = videoUrl.substring(0, videoUrl.indexOf(URL_TYPE_EMBED) + 1);

            String html_response = SimpleHttpClient.GET(videoUrl);
            String videoId = Util.Html.getTag(html_response, KEY_EMBED_TO_VIDEO_ID, "\"");

            if (videoId != null && videoId.contains("?")) {
                videoId = videoId.substring(0, videoId.indexOf("?"));
            }

            return videoIdToVideoUrl(videoId);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return null;
    }

    public String videoIdToVideoUrl(String videoId){
        if (videoId == null) {
            return null;
        }

        String videoUrl = null;
        try {
            parse(SimpleHttpClient.GET(URL_YOUTUBE_GET_VIDEO_INFO + videoId + URL_YOUTUBE_GET_VIDEO_INFO_PARAMS + videoId));

            String is_live = getInfo(KEY_LIVE_STREAM);

            if(is_live != null && is_live.contentEquals("1")) {
                videoUrl = getHlsManifestUrl(getInfo(KEY_PLAYER_RESPONSE));
            } else {
                videoUrl = null;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return videoUrl;
    }

    public String getInfo(String key)
    {
        return kvpList.get(key);
    }

    public void printAll()
    {
        System.out.println("TOTAL VARIABLES=" + kvpList.size());

        for(Map.Entry<String, String> entry : kvpList.entrySet())
        {
            System.out.print( "" + entry.getKey() + "=");
            System.out.println("" + entry.getValue() + "");
        }
    }

    private void parse(String data) throws UnsupportedEncodingException
    {
        String[] splits = data.split("&");
        String kvpStr = "";

        if(splits.length < 1)
        {
            return;
        }

        kvpList.clear();

        for(int i = 0; i < splits.length; ++i)
        {
            kvpStr = splits[i];

            try
            {
                // Data is encoded multiple times
                kvpStr = URLDecoder.decode(kvpStr, SimpleHttpClient.ENCODING_UTF_8);
                kvpStr = URLDecoder.decode(kvpStr, SimpleHttpClient.ENCODING_UTF_8);

                String[] kvpSplits = kvpStr.split("=", 2);

                if(kvpSplits.length == 2)
                {
                    kvpList.put(kvpSplits[0], kvpSplits[1]);
                }
                else if(kvpSplits.length == 1)
                {
                    kvpList.put(kvpSplits[0], "");
                }
            }
            catch (UnsupportedEncodingException ex)
            {
                throw ex;
            }
        }
    }

    private String getHlsManifestUrl(String player_response) {
        String videoUrl = null;

        try {
            JSONObject jsonObject = new JSONObject(player_response);
            jsonObject = jsonObject.getJSONObject(KEY_STREAMING_DATA);
            videoUrl = jsonObject.getString(KEY_HLS_MANIFEST_URL);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing player_response");
        }

        return videoUrl;
    }
}
