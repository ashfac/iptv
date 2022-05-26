package com.example.android.sampletvinput.feeds;

import android.media.tv.TvContract;
import android.net.Uri;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.google.android.exoplayer.util.Util;
import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArconaiTvFeed {

    private static final String ARCONAI_TV_URL = "https://www.arconaitv.us/";
    private static final String ARCONAI_TV_LOGO = "https://raw.githubusercontent.com/piplongrun/arconaitv.bundle/master/Contents/Resources/icon-default.jpg";
    private static final String[] CHANNEL_NUMBERS = {"101-", "102-", "103-"};
    private static final int[] SORT_ORDER = {2, 3, 1};


    private static String mHtmlResponse = null;
    private static List<StreamItem> mStreamsList = new ArrayList<>();
    private static int[] mCategoryChannels = {0, 0, 0};

    public static boolean init() throws IOException {
        mStreamsList.clear();

        mHtmlResponse = SimpleHttpClient.GET(ARCONAI_TV_URL + "index.php");

        String[] categories = mHtmlResponse.split("div class=\"stream-nav ");

        for(int i = 0; i < SORT_ORDER.length; i++) {
            String[] streams = categories[SORT_ORDER[i]].split("stream.php\\?");
            int start_index = (SORT_ORDER[i] == 0) ? 3 : 1;

            for(int j=start_index; j<streams.length; j++) {
                mStreamsList.add(getNextStream(String.format("%s%d", CHANNEL_NUMBERS[i], j - start_index +1), streams[j]));
            }

            if( i < SORT_ORDER.length - 1) {
                mCategoryChannels[i+1] = mCategoryChannels[i] + streams.length - start_index;
            }
        }

        return (mStreamsList.size() > 0);
    }

    public static List<Channel> getChannels() {
        List<Channel> channelList = new ArrayList<>();

        for(int i=0; i<mStreamsList.size(); i++) {
            InternalProviderData internalProviderData = new InternalProviderData();
            internalProviderData.setRepeatable(true);

            Channel channel = new Channel.Builder()
                    .setDisplayName(mStreamsList.get(i).getTitle())
                    .setDisplayNumber(mStreamsList.get(i).getmChannelNumber())
                    .setChannelLogo(mStreamsList.get(i).getLogoUrl())
                    .setOriginalNetworkId(100 + i)
                    .setInternalProviderData(internalProviderData)
                    .build();
            channelList.add(channel);
        }

        return channelList;
    }

    public static List<Program> getProgramsForChannel(Channel channel) {
        String displayNumber = channel.getDisplayNumber();
        int category = 0;

        for(int i=0; i < CHANNEL_NUMBERS.length; i++) {
            if(displayNumber.indexOf(CHANNEL_NUMBERS[i]) != -1) {
                category = i;
                break;
            }
        }

        int channelNumber = mCategoryChannels[category] + Integer.parseInt(displayNumber.substring(4));
        StreamItem streamItem = mStreamsList.get(channelNumber-1);
        int programStartTime = 0;
        int programEndTime = 24 * 60 * 60 * 1000;

        List<Program> programList = new ArrayList<>();
        InternalProviderData internalProviderData = new InternalProviderData();
        internalProviderData.setVideoType(Util.TYPE_HLS);
        internalProviderData.setVideoUrl(ARCONAI_TV_URL + "stream.php?id=" + streamItem.getProgramId());
        programList.add(new Program.Builder()
                .setTitle(streamItem.getTitle())
                .setStartTimeUtcMillis(programStartTime)
                .setEndTimeUtcMillis(programEndTime)
                .setDescription(streamItem.getProgramDescription())
                .setCanonicalGenres(new String[] {TvContract.Programs.Genres.MOVIES})
                .setPosterArtUri(streamItem.getLogoUrl())
                .setThumbnailUri(streamItem.getLogoUrl())
                .setInternalProviderData(internalProviderData)
                .build());

        return programList;
    }

    public static boolean isAcronaiChannel(Channel channel) {
        boolean found = false;
        String displayNumber = channel.getDisplayNumber();

        for(String channelNumber : CHANNEL_NUMBERS) {
            if(displayNumber.indexOf(channelNumber) != -1) {
                found = true;
                break;
            }
        }

        return found;
    }

    private static StreamItem getNextStream(String channelNumber, String line) {
        line = line.substring(0, line.indexOf("/div>"));
        String programId = line.substring(3, line.indexOf('\''));

        String programDescription = line.substring(line.indexOf("title=") + 7);
        programDescription = programDescription.substring(0, programDescription.indexOf('\''));

        String logoUrl = ARCONAI_TV_LOGO;
        int logo_index = line.indexOf("img src=");
        if(logo_index != -1) {
            logoUrl = line.substring(logo_index + 10);
            logoUrl = ARCONAI_TV_URL + logoUrl.substring(0, logoUrl.indexOf("\'"));
        }

        int title_index = line.indexOf("alt=");
        String title = programDescription;

        if(title_index != -1) {
            title = line.substring(title_index + 4);

            if(title.charAt(0) == '\'' && title.charAt(1) == '\'') {
                title = programDescription;
            } else {
                title = title.substring(1);
                title = title.substring(0, title.indexOf('\''));
            }
        }


        return( new StreamItem(channelNumber, title, programId, programDescription, logoUrl));
    }

    public static class StreamItem{
        private String mChannelNumber;
        private String mTitle;
        private String mProgramId;
        private String mProgramDescription;
        private String mLogoUrl;

        public StreamItem(String channelNumber, String title, String programId, String programDescription, String logoUrl) {
            mChannelNumber = channelNumber;
            mTitle = title;
            mProgramId = programId;
            mProgramDescription = programDescription;
            mLogoUrl = logoUrl;
        }

        public String getmChannelNumber() {
            return mChannelNumber;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getProgramId() {
            return mProgramId;
        }

        public String getProgramDescription() {
            return mProgramDescription;
        }

        public String getLogoUrl() {
            return mLogoUrl;
        }
    }
}
