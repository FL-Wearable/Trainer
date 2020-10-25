package fl.wearable.autosport.lib;

import java.nio.charset.StandardCharsets;

public class BitUtility {
    public static byte[] getBytes(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    public static byte[] getBytes(short value) {
        return new byte[] {
                (byte)(value >>> 8),
                (byte)value
        };
    }

    public static byte[] getBytes(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value
        };
    }

    public static byte[] getBytes(long value) {
        return new byte[] {
                (byte)(value >>> 56),
                (byte)(value >>> 48),
                (byte)(value >>> 40),
                (byte)(value >>> 32),
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value
        };
    }

    public static byte[] getBytes(float value) {
        return getBytes(Float.floatToRawIntBits(value));
    }

    public static byte[] getBytes(double value) {
        return getBytes(Double.doubleToRawLongBits(value));
    }

    public static int getInt(byte[] buffer, int offset) {
        return (0xff & buffer[offset]) << 24
                | (0xff & buffer[offset + 1]) << 16
                | (0xff & buffer[offset + 2]) << 8
                | (0xff & buffer[offset + 3]);
    }

    public static short getShort(byte[] buffer, int offset) {
        return (short)((0xff & buffer[offset]) << 8
                | (0xff & buffer[offset + 1]));
    }

    public static long getLong(byte[] buffer, int offset) {
        return (long)(0xff & buffer[offset]) << 56
                | (long)(0xff & buffer[offset + 1]) << 48
                | (long)(0xff & buffer[offset + 2]) << 40
                | (long)(0xff & buffer[offset + 3]) << 32
                | (long)(0xff & buffer[offset + 4]) << 24
                | (long)(0xff & buffer[offset + 5]) << 16
                | (long)(0xff & buffer[offset + 6]) << 8
                | (long)(0xff & buffer[offset + 7]);
    }

    public static float getFloat(byte[] buffer, int offset) {
        return Float.intBitsToFloat(getInt(buffer, offset));
    }
}
