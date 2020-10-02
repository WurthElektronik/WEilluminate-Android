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

package com.eisos.android;

import com.eisos.android.utils.Parser;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ParsingTest {

    @Test
    public void parseHexStringToBinary() {
        String hex = "010800FF";
        byte[] val = new byte[hex.length() / 2];
        for (int i = 0; i < val.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hex.substring(index, index + 2), 16);
            val[i] = (byte) j;
        }
        assertEquals(Arrays.toString(val), Arrays.toString(Parser.parseHexBinary(hex)));
    }

    @Test
    public void parseBytesToHexString() {
        String hex = "010800FF";
        byte[] array = Parser.parseHexBinary(hex);
        assertEquals(hex, Parser.bytesToHex(array));
    }

    @Test
    public void convertSeekBarValues() {
        int value = 50;
        int brightness = 50;
        assertEquals(25, Parser.convertSeekBarValue(value, brightness));

        int value2 = 70;
        int brightness2 = 50;
        assertEquals(35, Parser.convertSeekBarValue(value2, brightness2));

        int value3 = 100;
        int brightness3 = 10;
        assertEquals(10, Parser.convertSeekBarValue(value3, brightness3));
    }

    @Test
    public void charToByteArray() {
        String s = "0XFF";
        char[] chars = s.toCharArray();
        byte[] bytes = s.getBytes();
        assertEquals(Arrays.toString(bytes), Arrays.toString(Parser.charToByte(chars)));
    }

    @Test
    public void convertIntToHexValue() {
        String str = "FF";
        assertEquals(str, Parser.convertToHexValue(100));

        String str2 = "02";
        assertEquals(str2, Parser.convertToHexValue(1));

        String str3 = "05";
        assertEquals(str3, Parser.convertToHexValue(2));

        String str4 = "07";
        assertEquals(str4, Parser.convertToHexValue(3));
    }

    @Test
    public void convertChannelValueToPercent() {
        int perc = 100;
        assertEquals(perc, Parser.convertChannelToPercent(255));

        int perc2 = 50;
        assertEquals(perc2, Parser.convertChannelToPercent(128));

        int perc3 = 25;
        assertEquals(perc3, Parser.convertChannelToPercent(64));
    }

    @Test
    public void getPWMValue() {
        byte[] value = new byte[5];
        value[0] = (byte) 0x01;
        value[1] = (byte) 0x01;
        value[2] = (byte) 0x00;
        value[3] = (byte) 0x01;
        value[4] = (byte) 0xFF;
        assertEquals(255, Parser.getPWMValue(value).intValue());

        byte[] value2 = new byte[5];
        value2[0] = (byte) 0x01;
        value2[1] = (byte) 0x01;
        value2[2] = (byte) 0x00;
        value2[3] = (byte) 0x01;
        value2[4] = (byte) 0x0A;
        assertEquals(10, Parser.getPWMValue(value2).intValue());
    }
}
