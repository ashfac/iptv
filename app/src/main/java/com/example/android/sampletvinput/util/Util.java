package com.example.android.sampletvinput.util;

import android.content.Context;
import android.os.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Util {
    public static String USER_AGENT_FIREFOX = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:62.0) Gecko/20100101 Firefox/62.0";
    public static String HTTP_REQUEST_HEADERS = "<|http|request|headers|>";
    public static String HTTP_REQUEST_HEADER_SEPARATOR = ":=>";
    public static final String ENCODING_UTF_8 = "UTF-8";

    public static class DateTime {
        public static final long MS_IN_ONE_HOUR = 60 * 60 * 1000;
        public static final long MS_IN_24_HOUR = 24 * MS_IN_ONE_HOUR;
        public static final String DATE_FORMAT_12HR = "h:mm a";
        public static final String DATE_FORMAT_24HR = "HH:mm";

        public static long getMidnightTimeMs() {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTimeInMillis();
        }

        public static long getMidnightTimeMsUtc() {
            return ((System.currentTimeMillis() / MS_IN_24_HOUR) * MS_IN_24_HOUR);
        }

        public static String getCurrentDayLong() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE");
            Date date = new Date();
            return simpleDateFormat.format(date);
        }

        public static long convertTimeMsHHmmss(String timeHHmmss) {
            String[] timeNow = timeHHmmss.split(":");

            long time = 0;

            for(int i = 0; i < timeNow.length - 1; i++) {
                time = 60 * (time + Long.parseLong(timeNow[i]));
            }

            return (1000 * (time + Long.parseLong(timeNow[timeNow.length - 1])));
        }

        public static long getTimeMs12Hr(String dateTime, String dateFormat) {
            try {
                SimpleDateFormat dateParser = new SimpleDateFormat(DATE_FORMAT_12HR);

                Date date = dateParser.parse(dateTime);
                SimpleDateFormat dateFormater = new SimpleDateFormat(DATE_FORMAT_24HR);

                return date.getTime();
            } catch (ParseException e) {
                System.err.println("Cannot parse this time string !");
            }
            return 0;
        }
    }

    public static class Html {
        public static String getTag(String line, String begin, String end) {
            String tag = null;

            if(line != null && line.contains(begin)) {
                tag = line.substring(line.indexOf(begin) + begin.length());

                if(tag.contains(end)) {
                    tag = tag.substring(0, tag.indexOf(end));
                } else {
                    tag = null;
                }
            }
            return tag;
        }

        public static String getTagReverse(String line, String begin, String end) {
            String tag = null;

            if(line != null && line.contains(end)) {
                tag = line.substring(0, line.indexOf(end));

                if(tag.contains(begin)) {
                    tag = tag.substring(tag.indexOf(begin)+1);
                } else {
                    tag = null;
                }
            }
            return tag;
        }

        public static String getTag(String line, String begin, int count) {
            String tag = null;

            if(line != null && line.contains(begin)) {
                int beginIndex = line.indexOf(begin) + begin.length();
                tag = line.substring(beginIndex, beginIndex + count);
            }
            return tag;
        }
    }

    public static class StringHelper{
        public static String StreamtoString(InputStream in) throws IOException
        {
            StringBuilder sb = new StringBuilder(8192);
            byte[] b = new byte[1024];
            int bytesRead = 0;

            while (true)
            {
                bytesRead = in.read(b);
                if (bytesRead < 0)
                {
                    break;
                }
                String s = new String(b, 0, bytesRead, ENCODING_UTF_8);
                sb.append(s);
            }

            return sb.toString();
        }
    }

    public static class FileSystem{
        public static String getDataDirectory(Context context) {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/iptv/";

            //context.getApplicationInfo().dataDir + "/data";
        }

        public static String getPlaylistPath(Context context) {
            return getDataDirectory(context) + "iptv_channels.m3u8";
        }

        public static String getEpgDirectory(Context context) {
            return getDataDirectory(context) + "epg/";
        }
    }
}
