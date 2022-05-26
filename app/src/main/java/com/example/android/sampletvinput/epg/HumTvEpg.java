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
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HumTvEpg {
    private static final String TAG = "HumTvEpg";
    private static final String EPG_URL_IDENTIFIER = "hum.tv";
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
                String epgString = SimpleHttpClient.GET(epgUrl);

                if(epgString.length() > 0) {
                    String[] dailySchedule = epgString.split("tabpanel");

                    if (dailySchedule.length == 8) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE");
                        Date date = new Date();
                        String dayOfTheWeek = simpleDateFormat.format(date);

                        int startDay = 0;
                        while (startDay < DAYS_OF_WEEK.length) {
                            if (dayOfTheWeek.contentEquals(DAYS_OF_WEEK[startDay])) {
                                break;
                            }

                            startDay++;
                        }

                        for (int currentDay = 0; currentDay < DAYS_OF_WEEK.length; currentDay++) {
                            int epgDay = (startDay + currentDay) % DAYS_OF_WEEK.length;

                            long timeCorrection = ((System.currentTimeMillis() / MS_IN_24_HOUR) * MS_IN_24_HOUR)
                                    + ((currentDay * MS_IN_24_HOUR) + TIME_ZONE_CORRECTION);

                            String[] hourlySchedule = dailySchedule[epgDay + 1].split("col-sm-6 col-md-6 col-lg-4");

                            if(hourlySchedule.length > 1) {
                                for (int i = 1; i < hourlySchedule.length; i++) {
                                    Program program = createProgram(channelId, logoUrl, internalProviderData,
                                            hourlySchedule[i],
                                            (i == hourlySchedule.length - 1) ? null : hourlySchedule[i + 1],
                                            timeCorrection);

                                    if (program != null) {
                                        programs.add(program);
                                    }
                                }
                            }
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
                                         String currentProgram,
                                         String nextProgram,
                                         long timeCorrection) {

        String epgProgramTitle = getTag(currentProgram, "title=\"", "\"");
        String epgLogo = getTag(currentProgram, "<img src=\"", "\"");
        String epgStartTime = getTag(currentProgram, "<h4>", "&nbsp");
        String epgEndTime = (nextProgram == null) ? "24:00" : getTag(nextProgram, "<h4>", "&nbsp");

        long epgStartTimeMs = convertTimeMs(epgStartTime) + timeCorrection;
        long epgEndTimeMs = convertTimeMs(epgEndTime) + timeCorrection;

        Program program = null;
        try {
            program = new Program.Builder()
                    .setChannelId(channelId.hashCode())
                    .setTitle(epgProgramTitle)
                    .setDescription(" ")
                    .setPosterArtUri(epgLogo)
                    .setStartTimeUtcMillis(epgStartTimeMs)
                    .setEndTimeUtcMillis(epgEndTimeMs)
                    .setInternalProviderData(internalProviderData).build();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }

        return program;

    }

    private static String getTag(String line, String begin, String end) {
        String tag = null;

        if(line.contains(begin)) {
            tag = line.substring(line.indexOf(begin) + begin.length());

            if(tag.contains(end)) {
                tag = tag.substring(0, tag.indexOf(end));
            } else {
                tag = null;
            }
        }
        return tag;
    }

    private static long convertTimeMs(String timeHHmm) {
        timeHHmm = timeHHmm.replace(" ", "");

        String[] timeNow = timeHHmm.split(":");

        long time = (60 * Long.parseLong(timeNow[0]) )
                + Long.parseLong(timeNow[1]);

        return (1000 *60 * time);
    }
}
