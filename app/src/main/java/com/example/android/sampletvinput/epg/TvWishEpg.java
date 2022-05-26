package com.example.android.sampletvinput.epg;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TvWishEpg {
    public static final String TAG = "TvWishEpg";
    public static final String EPG_URL_IDENTIFIER = "tvwish.com";

    public static final String EPG_URL = "https://tvwish.com//Channels/DayJson/";
    public static final String EPG_DATE = "?dt=";

    private static final String EPG_DATE_FORMAT = "MM-dd-yyyy";
    private static final long MS_IN_HALF_HOUR = 30 * 60 * 1000;
    private static final long MS_IN_ONE_HOUR = 60 * 60 * 1000;
    private static final long MS_IN_24_HOUR = 24 * MS_IN_ONE_HOUR;
    private static final long TIME_ZONE_CORRECTION = - ((5 * MS_IN_ONE_HOUR) + MS_IN_HALF_HOUR);

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
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(EPG_DATE_FORMAT);

                long currentDay;
                for (currentDay = 0; currentDay < 3; currentDay++) {
                    Date date = new Date(System.currentTimeMillis() + (currentDay * MS_IN_24_HOUR));
                    String dateToday = simpleDateFormat.format(date);
                    long timeCorrection = ((System.currentTimeMillis() / MS_IN_24_HOUR) * MS_IN_24_HOUR)
                            + ((currentDay * MS_IN_24_HOUR) + TIME_ZONE_CORRECTION);

                    String epgString = SimpleHttpClient.GET(EPG_URL + epgUrl.substring(epgUrl.lastIndexOf("/")+1) + EPG_DATE + dateToday);

                    if(epgString != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(epgString);
                            JSONArray jsonArray = jsonObject.getJSONArray("programs");

                            for (int i = 0; i < jsonArray.length(); i++) {
                                Program program = createProgram(channelId, logoUrl, internalProviderData,
                                        jsonArray.getJSONObject(i),
                                        (i == jsonArray.length() - 1) ? null : jsonArray.getJSONObject(i+1),
                                        timeCorrection);

                                if(program != null) {
                                    programs.add(program);
                                }
                            }

                        } catch (JSONException e) {
                            Log.e(TAG, "JsonEpg parsing error: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "EPG Url GET failed: " + e.getMessage());
            }
        }

        return programs;
    }
    private static Program createProgram(String channelId,
                                         String logoUrl,
                                         InternalProviderData internalProviderData,
                                         JSONObject jsonObject,
                                         JSONObject jsonObjectNext,
                                         long timeCorrection) throws JSONException {

        String epgStartTime = jsonObject.getString("startTime");
        String epgEndTime = (jsonObjectNext == null) ? "24:00:00" : jsonObjectNext.getString("startTime");

        JSONObject jsonProgram = jsonObject.getJSONObject("program");
        String epgProgramTitle = jsonProgram.getString("movieName");
        String epgProgramSummary = jsonProgram.getString("synopsis");
        logoUrl = jsonProgram.getString("pictureUrl");

        long epgStartTimeMs = Util.DateTime.convertTimeMsHHmmss(epgStartTime) + timeCorrection;
        long epgEndTimeMs = Util.DateTime.convertTimeMsHHmmss(epgEndTime) + timeCorrection;

        Program program = null;
        try {
            program = new Program.Builder()
                    .setChannelId(channelId.hashCode())
                    .setTitle(epgProgramTitle)
                    .setDescription(epgProgramSummary)
                    .setPosterArtUri(logoUrl)
                    .setStartTimeUtcMillis(epgStartTimeMs)
                    .setEndTimeUtcMillis(epgEndTimeMs)
                    .setInternalProviderData(internalProviderData).build();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }

        return program;

    }
}
