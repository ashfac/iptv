package com.example.android.sampletvinput.util;

public class JavaScriptUnpacker {
    private static String alphabet[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};

    public static String unpack(String packed) {

        String func = packed.substring(packed.indexOf("}(") + 1, packed.indexOf(";',") + 3 - 3);

        int indexOfBase = packed.indexOf(";',") + 3;

        String temp = packed.substring(indexOfBase);
        temp = temp.substring(0, temp.indexOf(",\'"));

        String base = temp.substring(0, temp.indexOf(','));
        String wordCount = temp.substring(temp.indexOf(',') + 1);

        String dict = packed.substring(indexOfBase + temp.length() + 4 , packed.length());
        dict = dict.substring(0, dict.indexOf("\'.split"));

        String[] vars = dict.split("\\|");

        return decodeFunc(func, vars, base, wordCount);
    }

    private static String decodeFunc(String func, String[] vars, String sBase, String sWordCount) {
        int base = 0;
        int wordCount = 0;

        try {
            base = Integer.parseInt(sBase);
            wordCount = Integer.parseInt(sWordCount);
        } catch(NumberFormatException e) {

        }

        if(vars.length == wordCount && wordCount <= alphabet.length) {
            for(int i=0; i<wordCount; i++) {
                func = func.replaceAll("\\b" + alphabet[i] + "\\b", vars[i]);
            }
            return func;
        }

        return null;
    }
}
