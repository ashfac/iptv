package com.example.android.sampletvinput.epg;

import android.net.Uri;
import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class AlJazeeraEpg {
    public static final String TAG = "AlJazeeraEpg";
    public static final String EPG_URL_IDENTIFIER = "aljazeera.com";
    public static final String EPG_DATE_FORMAT = "dd-MMM-yyyy HH:mm:ss";

    private static final long MS_IN_HALF_HOUR = 30 * 60 * 1000;
    private static final long MS_IN_ONE_HOUR = 2 * MS_IN_HALF_HOUR;

    public static String getEpgUrlIdentifier() {
            return EPG_URL_IDENTIFIER;
        }

        public static List<Program> getAllPrograms(String channelNumber, String channelName, String videoUrl, String logoUrl, String epgUrl) {
            String channelId = channelNumber + "-" + channelName;
            InternalProviderData internalProviderData = new InternalProviderData();
            internalProviderData.setVideoType(TvContractUtils.SOURCE_TYPE_HLS);
            internalProviderData.setVideoUrl(videoUrl);
            List<Program> programs = new ArrayList<>();

            if (epgUrl != null) {
                String queryParams = new Uri.Builder()
                        .appendQueryParameter("Origin", "https://www.aljazeera.com")
                        .appendQueryParameter("Referer", "https://www.aljazeera.com/watch_now/epgschedule.html")
                        .appendQueryParameter("Content-Type", "application/json; charset=utf-8")
                        .appendQueryParameter("Accept", "application/json, text/javascript, */*; q=0.01")
                        .appendQueryParameter("X-Requested-With", "XMLHttpRequest")
                        .appendQueryParameter("Accept-Language", "en-IE")
                        .appendQueryParameter("Accept-Encoding", "gzip, deflate, br")
                        .appendQueryParameter("Host", "www.aljazeera.com")
                        .appendQueryParameter("Cookie", "AJEUserLocation=NL; AJEUserRegion=South Holland; AJEUserCity=The Hague; AJEUserCountry=Netherlands")
                        .build().getEncodedQuery();

                try {
                    String epgString = SimpleHttpClient.POST(epgUrl, queryParams);

                    if(epgString != null && epgString.length() > 0) {
                        try {
                            JSONObject jsonObject = new JSONObject(epgString);
                            jsonObject = jsonObject.getJSONObject("Schedule");
                            JSONArray jsonArray = jsonObject.getJSONArray("Programs");

                            for (int i = 0; i < jsonArray.length()-1; i++) {
                                Program program = createProgram(channelId, logoUrl, internalProviderData, jsonArray.getJSONObject(i), jsonArray.getJSONObject(i+1));

                                if(program != null) {
                                    programs.add(program);
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JsonEpg parsing error: " + e.getMessage());
                        }
                    }

                } catch (IOException e) {
                    Log.e(TAG, "EPG Url GET failed: " + e.getMessage());
                }
            }

            return programs;
        }

    private static Program createProgram(String channelId, String logoUrl, InternalProviderData internalProviderData, JSONObject jsonObject, JSONObject jsonObjectNext) throws JSONException
    {
        Program program = null;
        String epgProgramTitle = jsonObject.getString("SeriesTitle");
        String epgEpisodeTitle = jsonObject.getString("Title");
        String epgSummary = jsonObject.getString("Synopsis");
        String epgStartTime = jsonObject.getString("TVGuideDateTime");
        String epgEndTime = jsonObjectNext.getString("TVGuideDateTime");

        long epgStartTimeMs = 0;
        long epgEndTimeMs = 0;

        try {
            DateFormat dateFormat = new SimpleDateFormat(EPG_DATE_FORMAT);
            Date date = dateFormat.parse(epgStartTime);
            epgStartTimeMs = date.getTime() + (2 * MS_IN_ONE_HOUR);

            date = dateFormat.parse(epgEndTime);
            epgEndTimeMs = date.getTime() + (2 * MS_IN_ONE_HOUR);
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage());
        }

        if (epgStartTimeMs == 0) {
            epgStartTimeMs = System.currentTimeMillis();
            epgEndTimeMs = epgStartTimeMs + MS_IN_ONE_HOUR;
        }

        program = new Program.Builder()
                .setChannelId(channelId.hashCode())
                .setTitle(epgProgramTitle)
                .setEpisodeTitle(epgEpisodeTitle)
                .setSeasonNumber(1)
                .setEpisodeNumber(1)
                .setDescription(epgSummary)
                .setPosterArtUri(logoUrl)
                .setStartTimeUtcMillis(epgStartTimeMs)
                .setEndTimeUtcMillis(epgEndTimeMs)
                .setInternalProviderData(internalProviderData)
                .build();

        return program;
    }

    private static long convertTimeMs(String timeHHmmss) {
        String[] timeNow = timeHHmmss.split(":");

        long time = 0;

        for(int i = 0; i < timeNow.length - 1; i++) {
            time = 60 * (time + Long.parseLong(timeNow[i]));
        }

        return (1000 * (time + Long.parseLong(timeNow[timeNow.length - 1])));
    }
}
