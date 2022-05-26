package com.example.android.sampletvinput.epg;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
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

public class PtvEpg {
    private static final String TAG = "PtvEpg";
    private static final String EPG_URL_IDENTIFIER = "ptv.com.pk";
    private static final String EPG_URL_DAY = "&nameofday=";
    private static final String[] DAYS_OF_WEEK = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final long MS_IN_ONE_HOUR = 60 * 60 * 1000;
    private static final long MS_IN_24_HOUR = 24 * MS_IN_ONE_HOUR;
    private static final long TIME_ZONE_CORRECTION = - (5 * MS_IN_ONE_HOUR);

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
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE");
                Date date = new Date();
                String dayOfTheWeek = simpleDateFormat.format(date);

                int startDay = 0;
                while(startDay < DAYS_OF_WEEK.length) {
                    if(dayOfTheWeek.contentEquals(DAYS_OF_WEEK[startDay])) {
                        break;
                    }

                    startDay++;
                }

                for (int currentDay = 0; currentDay < DAYS_OF_WEEK.length; currentDay++) {
                    String epgDay = DAYS_OF_WEEK[(startDay + currentDay) % DAYS_OF_WEEK.length];

                    String epgString = SimpleHttpClient.GET(epgUrl + EPG_URL_DAY + epgDay);

                    if(epgString != null) {
                        try {
                            long timeCorrection = ((System.currentTimeMillis() / MS_IN_24_HOUR) * MS_IN_24_HOUR)
                                    + ((currentDay * MS_IN_24_HOUR) + TIME_ZONE_CORRECTION);

                            JSONArray jsonArray = new JSONArray(epgString);
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

        String epgProgramTitle = jsonObject.getString("programName");
        String epgStartTime = jsonObject.getString("programTime");
        String epgEndTime = (jsonObjectNext == null) ? "2400" : jsonObjectNext.getString("programTime");

        long epgStartTimeMs = convertTimeMs(epgStartTime) + timeCorrection;
        long epgEndTimeMs = convertTimeMs(epgEndTime) + timeCorrection;

        Program program = null;
        try {
            program = new Program.Builder()
                    .setChannelId(channelId.hashCode())
                    .setTitle(epgProgramTitle)
                    .setDescription(" ")
                    .setPosterArtUri(logoUrl)
                    .setStartTimeUtcMillis(epgStartTimeMs)
                    .setEndTimeUtcMillis(epgEndTimeMs)
                    .setInternalProviderData(internalProviderData).build();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }

        return program;

    }

    private static long convertTimeMs(String timeHHmm) {
        timeHHmm = timeHHmm.replace(" ", "");

        long time = (60 * Long.parseLong(timeHHmm.substring(0, 2)) )
                + Long.parseLong(timeHHmm.substring(2, 4));

        return (1000 *60 * time);
    }
}
