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
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class TvWizInEpg {
    public static final String TAG = "TvWizInEpg";
    public static final String EPG_URL_IDENTIFIER = "tvwiz.in";

    public static final String EPG_URL = "https://tvwiz.in/api/channelPrograms?pageno=0&pagesize=20&channelid=";

    public static String getEpgUrlIdentifier() {
        return EPG_URL_IDENTIFIER;
    }

    public static List<Program> getAllPrograms(String channelNumber, String channelName, String videoUrl, String epgUrl) {
        String channelId = channelNumber + "-" + channelName;
        InternalProviderData internalProviderData = new InternalProviderData();
        internalProviderData.setVideoType(TvContractUtils.SOURCE_TYPE_HLS);
        internalProviderData.setVideoUrl(videoUrl);
        List<Program> programs = new ArrayList<>();

        if (epgUrl != null) {
            try {
                String epgString = SimpleHttpClient.GET(EPG_URL + epgUrl.substring(epgUrl.lastIndexOf("/")+1));

                if(epgString != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(epgString);
                        JSONArray results = jsonObject.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject result = results.getJSONObject(i);

                            long epgStartTimeMs = Long.parseLong(result.getString("starttime"));
                            long epgEndTimeMs = Long.parseLong(result.getString("endtime"));

                            JSONObject epgProgram = result.getJSONObject("program");

                            String epgProgramTitle;
                            String epgEpisodeTitle = "";
                            int epgSeasonNum = Integer.parseInt(epgProgram.getString("seasonno"));
                            int epgEpisodeNum = Integer.parseInt(epgProgram.getString("episodeno"));

                            if(epgProgram.has("mainprogramname")) {
                                epgProgramTitle = epgProgram.getString("mainprogramname");
                                epgEpisodeTitle = epgProgram.getString("name");
                            } else {
                                epgProgramTitle = epgProgram.getString("name");
                            }

                            String epgProgramDescription = (epgProgram.has("summary")) ?
                                    epgProgram.getString("summary") : epgProgramTitle;

                            String epgPosterUrl;
                            if(epgProgram.has("poster")) {
                                epgPosterUrl = epgProgram.getString("poster");
                            } else {
                                epgPosterUrl = epgProgram.getString("thumbnail");
                            }

                            Program.Builder builder = new Program.Builder()
                                    .setChannelId(channelId.hashCode())
                                    .setTitle(epgProgramTitle)
                                    .setDescription(epgProgramDescription)
                                    .setPosterArtUri(epgPosterUrl)
                                    .setStartTimeUtcMillis(epgStartTimeMs)
                                    .setEndTimeUtcMillis(epgEndTimeMs)
                                    .setInternalProviderData(internalProviderData);

                            if(epgSeasonNum > 0 && epgEpisodeNum > 0) {
                                builder.setEpisodeTitle(epgEpisodeTitle)
                                        .setSeasonNumber(epgSeasonNum)
                                        .setEpisodeNumber(epgEpisodeNum);
                            }
                            programs.add(builder.build());
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
}
