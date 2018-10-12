package com.example.android.sampletvinput.feeds;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.android.sampletvinput.epg.ElectronicProgramGuide;
import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class M3U8Feed {
    private static final String TAG = "M3U8Feed";

    public static final String EXT_M3U = "#EXTM3U";
    public static final String EXT_INF = "#EXTINF:";
    public static final String EXT_CHANNEL = "tvg-channel=";
    public static final String EXT_LOGO = "tvg-logo=";
    public static final String EXT_EPG = "tvg-epg=";
    public static final String EXT_URL_HTTP = "http://";
    public static final String EXT_URL_HTTPS = "https://";
    public static final String EPG_URL = "epg-url";

    private static final long MS_IN_ONE_HOUR = 60 * 60 * 1000;

    public static List<Channel> getChannels(Context context, @NonNull InputStream inputStream) {
        List<Channel> channels = new ArrayList<>();

        if(inputStream == null) {
            return channels;
        }

        String stream;
        try {
            stream = new Scanner(inputStream).useDelimiter("\\A").next();

            String linesArray[] = stream.split(EXT_INF);

            String channelNumber = "0";
            String channelName = null;
            String logoUrl = null;
            String epgUrl = null;
            String videoUrl = null;

            for (int i = 0; i < linesArray.length; i++) {
                String currLine = linesArray[i];
                if (currLine.contains(EXT_M3U)) {
                    // skip header of file
                } else {
                    String[] dataArray = currLine.split(",");

                    if (dataArray[0].contains(EXT_CHANNEL)) {
                        channelNumber = dataArray[0].substring(dataArray[0].indexOf(EXT_CHANNEL) + EXT_CHANNEL.length()+1);
                        channelNumber = channelNumber.substring(0, channelNumber.indexOf("\""));
                    } else {
                        // increment channel numnber for the next channel
                        channelNumber = Integer.toString(Integer.parseInt(channelNumber) + 1);
                    }

                    if (dataArray[0].contains(EXT_LOGO)) {
                        logoUrl = dataArray[0].substring(dataArray[0].indexOf(EXT_LOGO) + EXT_LOGO.length()+1);
                        logoUrl = logoUrl.substring(0, logoUrl.indexOf("\""));
                    }

                    if (dataArray[0].contains(EXT_EPG)) {
                        epgUrl = dataArray[0].substring(dataArray[0].indexOf(EXT_EPG) + EXT_EPG.length()+1);
                        epgUrl = epgUrl.substring(0, epgUrl.indexOf("\""));
                    }

                    try {
                        String ext_url = dataArray[1].contains(EXT_URL_HTTP) ? EXT_URL_HTTP : EXT_URL_HTTPS;
                        videoUrl = dataArray[1].substring(dataArray[1].indexOf(ext_url))
                                .replace("\r", "")
                                .replace("\n", "");

                        channelName = dataArray[1].substring(0, dataArray[1].indexOf(ext_url))
                                .replace("\r", "")
                                .replace("\n", "")
                                .trim();

                    } catch (Exception fdfd) {
                        Log.e(TAG, "Error: " + fdfd.fillInStackTrace());
                    }

                    if(channelNumber != null && channelName != null && logoUrl != null && videoUrl != null) {
                        channels.add(parseChannel(channelNumber, channelName, logoUrl, videoUrl, epgUrl));

                        // make sure that an empty #EXTINF listing in the m3u file does not
                        // result in being added up as a duplicate channel
                        channelName = null;

                        // make sure that the EPG url is not resued for the next channel
                        // if no url is provided in m3u file
                        epgUrl = null;
                    }
                }
            }
        } catch (NoSuchElementException e) {
            Log.e(TAG, e.getMessage());
        }

        return channels;
    }

    public static List<Program> getProgramsForChannel(Context context, Channel channel, long startMs, long endMs) {
        return ElectronicProgramGuide.getProgramsForChannel(context, channel, startMs, endMs);
    }

    private static Channel parseChannel(String channelNumber, String channelName, String logoUrl, String videoUrl, String epgUrl) {
        String channelId = channelNumber + "-" + channelName;

        InternalProviderData internalProviderData = new InternalProviderData();
        internalProviderData.setVideoUrl(videoUrl);

        try {
            internalProviderData.put(EXT_LOGO, logoUrl);
            internalProviderData.put(EPG_URL, epgUrl);
        } catch (InternalProviderData.ParseException e) {
            Log.e(TAG, "Error storing epg/logo url" + e.getMessage());
        }

        Channel.Builder builder = new Channel.Builder()
                .setDisplayName(channelName)
                .setDisplayNumber(channelNumber)
                .setChannelLogo(logoUrl)
                .setOriginalNetworkId(channelId.hashCode())
                .setInternalProviderData(internalProviderData)
                .setTransportStreamId(0)
                .setServiceId(0);
        return builder.build();
    }
}
