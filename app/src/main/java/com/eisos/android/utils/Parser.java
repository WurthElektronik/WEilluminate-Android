/*
 * __          ________        _  _____
 * \ \        / /  ____|      (_)/ ____|
 *  \ \  /\  / /| |__      ___ _| (___   ___  ___
 *   \ \/  \/ / |  __|    / _ \ |\___ \ / _ \/ __|
 *    \  /\  /  | |____  |  __/ |____) | (_) \__ \
 *     \/  \/   |______|  \___|_|_____/ \___/|___/
 *
 * Copyright Wuerth Elektronik eiSos 2019
 *
 */
package com.eisos.android.utils;

import java.util.Arrays;

/**
 * This class has methods to convert seek bar values to hex values
 * and the other way around.
 */
public final class Parser {

    private Parser() {}

    protected final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     *
     * @param chars The input characters
     * @return The characters as byte array
     */
    public static byte[] charToByte(char[] chars) {
        byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }

    /**
     *
     * @param bytes The input bytes
     * @return The String created out of the byte array
     */
    public static String bytesToString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    /**
     * @param s The Hexadecimal String
     * @return The byte array
     */
    public static byte[] parseHexBinary(String s) {
        final int len = s.length();

        // "111" is not a valid hex encoding.
        if (len % 2 != 0)
            throw new IllegalArgumentException("hexBinary needs to be even-length: " + s);

        byte[] out = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            int h = hexToBin(s.charAt(i));
            int l = hexToBin(s.charAt(i + 1));
            if (h == -1 || l == -1)
                throw new IllegalArgumentException("contains illegal character for hexBinary: " + s);

            out[i / 2] = (byte) (h * 16 + l);
        }

        return out;
    }

    /**
     *
     * @param ch The input character
     * @return The Hexadecimal value of the character
     */
    private static int hexToBin(char ch) {
        if ('0' <= ch && ch <= '9') return ch - '0';
        if ('A' <= ch && ch <= 'F') return ch - 'A' + 10;
        if ('a' <= ch && ch <= 'f') return ch - 'a' + 10;
        return -1;
    }

    /**
     * @param bytes The input byte array
     * @return The Hexadecimal String
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        final char[] out = new char[bytes.length * 3 - 1];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            out[j * 3] = HEX_ARRAY[v >>> 4];
            out[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            sb.append(out[j * 3]);
            sb.append(out[j * 3 + 1]);
        }
        return sb.toString();
    }

    /**
     * @param bytes The bytes of a PWM channel
     * @return The Integer value of the PWM value
     */
    public static Integer getPWMValue(byte[] bytes) {
        byte[] tmp = Arrays.copyOfRange(bytes, 4, bytes.length);
        String pwmValue = bytesToHex(tmp);
        int value = Integer.parseInt(pwmValue, 16);
        return value;
    }

    /**
     * @param value The Integer value of the seek bar
     * @return The value of the seek bar as Hex-String
     */
    public static String convertToHexValue(int value) {
        int tmp = (int) Math.floor((value / 100.0) * 255.0);
        String hex = String.format("%02X", tmp);
        return hex;
    }

    /**
     * Converts the value of a seek bar to a value between 0 and 255.
     * @param value the value of the seek bar
     * @return a value between 0 and 255
     */
    public static int convertChannelToOriginalValue(int value) {
        return (int) Math.floor((value / 100.0) * 255.0);
    }

    /**
     * @param valueCh The value of the channel
     * @return The value displayed in the Seek bar
     */
    public static int convertChannelToPercent(int valueCh) {
        int tmp = (int) Math.floor((valueCh / 255.0) * 100);
        return tmp;
    }

    /**
     *
     * @param value The value of the channel seek bar
     * @param brightness The value of the brightness seek bar
     * @return The value dependent of the brightness
     */
    public static int convertSeekBarValue(int value, int brightness) {
        int tmp = (int) Math.floor((Double.valueOf(brightness) / 100.0) * Double.valueOf(value));
        return tmp;
    }
}
