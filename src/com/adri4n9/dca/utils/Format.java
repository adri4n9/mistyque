package com.adri4n9.dca.utils;

/**
 * simple hexadecimal printing util function
 */
public class Format {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * converst a byte array into its string hexadecimal representation
     * @param bytes input array
     * @return String representation
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * convers  string to byte array
     * @param string input text in hex format
     * @return byte array
     */
    public static byte[] hexStringToByteArray(String string) {
        int len = string.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
                    + Character.digit(string.charAt(i+1), 16));
        }
        return data;
    }
}
