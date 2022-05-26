package com.example.android.sampletvinput.parsers;

import android.util.Log;

import com.example.android.sampletvinput.util.JavaScriptUnpacker;
import com.example.android.sampletvinput.util.JsUnpacker;
import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;

import java.io.IOException;

public class UstreaMixParser extends VideoUrlParser {
    private static String TAG = "UstreaMixParser";
    private static final String PARSER_ID = "ustreamix";
    private static final String ORG_URL = "https://ustreamix.org/go.php";
    private static final String TAG_ORG_URL = "go.php";
    private static final String TAG_REDIRECT = "location.replace(\"";
    private static final String TAG_EVAL_FUNC = "eval(function(p,a,c,k,e,d)";
    private static final String TAG_M3U8 = "|m3u8|";
    private static final String TAG_HOST = "host_tmg=\"";
    private static final String TAG_FILE_NAME = "file_name=\"";
    private static final String TAG_TOKEN = "jdtk=\"";
    private static final String TAG_REGION = "&region=eu";
    private static final String TAG_CDN = "&cdn=35fff7397f6548442f81f2aa4957a178";
    private static final int HTTP_TIMEOUT = 10000;

    @Override
    public String getParserId() {
        return PARSER_ID;
    }

    @Override
    public String parseVideoUrl(String videoUrl){
        try {
            String html_response = SimpleHttpClient.GET(videoUrl, Util.USER_AGENT_FIREFOX, HTTP_TIMEOUT);

            String stream_url = ORG_URL + Util.Html.getTag(html_response, TAG_ORG_URL, "\'");

            html_response = SimpleHttpClient.GET(stream_url, Util.USER_AGENT_FIREFOX, HTTP_TIMEOUT);

            stream_url = jsUnpack(extractEvalFunc(html_response, null));

            stream_url = Util.Html.getTag(stream_url, TAG_REDIRECT, "\"");

            html_response = SimpleHttpClient.GET(stream_url, Util.USER_AGENT_FIREFOX, HTTP_TIMEOUT);

            videoUrl = extractVideoUrl(html_response)
                    + Util.HTTP_REQUEST_HEADERS
                    + "Referer" + Util.HTTP_REQUEST_HEADER_SEPARATOR + stream_url + ";";

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return videoUrl ;
    }

    private static String extractEvalFunc(String data, String tag)
    {
        if (data == null) {
            return null;
        }

        int index = 0;

        if (tag == null) {
            index = data.indexOf(TAG_EVAL_FUNC);
        } else {
            index = data.indexOf(tag);
            if(index > 0) {
                index = data.lastIndexOf(TAG_EVAL_FUNC, index);
            } else {
                return null;
            }
        }

        String evalFunc = data.substring(index);
        evalFunc = evalFunc.substring(0, evalFunc.indexOf("{}))") + 4);

        return evalFunc;
    }

    private static String jsUnpack(String evalFunc) {
        if (evalFunc == null) {
            return null;
        }

        String unpacked = null;

        JsUnpacker jsUnpacker = new JsUnpacker(evalFunc);
        if(jsUnpacker.detect()) {
            unpacked = jsUnpacker.unpack();
        } else {
            Log.e(TAG, "Not P.A.C.K.E.D. coded");
        }

        return unpacked;
    }

    private static String extractVideoUrl(String html_response)
    {
        String videoUrl = null;

        String unpacked = jsUnpack(extractEvalFunc(html_response, TAG_M3U8));

        videoUrl = "https://"
                + Util.Html.getTag(unpacked, TAG_HOST, "\"")
                + "/" + Util.Html.getTag(unpacked, TAG_FILE_NAME, "\"")
                + "?token=" + Util.Html.getTag(unpacked, TAG_TOKEN, "\"")
                + TAG_REGION + TAG_CDN;

        return videoUrl;
    }
}
