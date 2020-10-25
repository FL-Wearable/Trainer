package fl.wearable.autosport.lib;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents the contents of a stored sport activity data.
 */
public class FileItem {
    private static final String TAG = FileItem.class.getSimpleName();

    private String fileName;
    private long startTimestampRtc;
    private long stopTimestampRtc;
    private long startNanoseconds;
    private long stopNanoseconds;
    private float avgHeartRate;
    private int maxHeartRate;
    private int totalSteps;
    private float avgStepRate;
    private float totalAscent;
    private float totalDescent;
    private float avgSpeed;
    private long fileSize;

    /**
     * The header of the file.
     */
    public static final byte[] HEADER = "//SPORT/TRACKER/".getBytes(StandardCharsets.UTF_8);
    /**
     * The implemented version of the file.
     */
    public static final short VERSION = 2;

    /**
     * Writes a field as a structure containing:
     * a start indicator,
     * the total length of the data
     * the data.
     * Please note, the individual byte arrays are not separated in any way. The implementor
     * needs to take care of their interpretation afterwards.
     *
     * @param os     output stream to write to
     * @param values byte arrays to be stored within of this field structure after each other
     * @throws IOException in case of an IO issue
     */
    public static void writeField(OutputStream os, byte[] ... values) throws IOException {
        int totalLength = 0;
        for (byte[] value : values) {
            totalLength += value.length;
        }

        os.write((byte)'#');
        os.write(BitUtility.getBytes(totalLength));

        for (byte[] value : values) {
            os.write(value);
        }
    }

    /**
     * Read a field structure as written by the {@link #writeField(OutputStream, byte[]...)} function.
     *
     * @param is the input stream to read from
     * @return the contained byte array (if multiple arrays have been stored, they will be returned as one concatenated array)
     * @throws IOException in case of an IO issue
     */
    public static byte[] readField(InputStream is) throws IOException {
        if (is.read() != (byte)'#') {
            throw new IOException("Beginning of a field not found.");
        }

        byte[] buffer = new byte[4];
        if (is.read(buffer) != buffer.length) {
            throw new IOException("Premature end of file.");
        }

        int totalLength = BitUtility.getInt(buffer, 0);
        buffer = new byte[totalLength];
        if (is.read(buffer) != buffer.length) {
            throw new IOException("Premature end of file.");
        }

        return buffer;
    }

    /**
     * Reads a stored sport activity from a file.
     *
     * @param file the file to read from
     * @throws IOException in case of an IO issue or file format mismatch
     */
    public FileItem(File file) throws IOException  {
        Log.d(TAG, "Reading file item " + file);
        this.fileName = file.getAbsolutePath();
        this.fileSize = file.length();

        FileInputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[HEADER.length];
        if (fis.read(buffer) != HEADER.length) {
            throw new IOException("Unknown file format (1).");
        }

        if (!Arrays.equals(buffer, HEADER)) {
            throw new IOException("Unknown file format (2).");
        }

        buffer = new byte[2];
        fis.read(buffer);
        int version = BitUtility.getShort(buffer, 0);
        if (version != VERSION) {
            throw new IOException("Unsupported version no. " + version);
        }

        while (true) {
            buffer = readField(fis);
            int offset = 0;
            short id = BitUtility.getShort(buffer, offset);
            offset += 2;

            switch (id) {
                case 0x1001: // start RTC
                    startTimestampRtc = BitUtility.getLong(buffer, offset);
                    break;

                case 0x1002: // stop RTC
                    stopTimestampRtc = BitUtility.getLong(buffer, offset);
                    break;

                case 0x1003: // start ns
                    startNanoseconds = BitUtility.getLong(buffer, offset);
                    break;

                case 0x1004: // stop ns
                    stopNanoseconds = BitUtility.getLong(buffer, offset);
                    break;

                case 0x1011: // avg heart rate (float)
                    avgHeartRate = BitUtility.getFloat(buffer, offset);
                    break;

                case 0x1012: // max heart rate (int)
                    maxHeartRate = BitUtility.getInt(buffer, offset);
                    break;

                case 0x1013: // total steps (int)
                    totalSteps = BitUtility.getInt(buffer, offset);
                    break;

                case 0x1014: // avg step rate (float)
                    avgStepRate = BitUtility.getFloat(buffer, offset);
                    break;

                case 0x1015: // total ascent (float)
                    totalAscent = BitUtility.getFloat(buffer, offset);
                    break;

                case 0x1016: // total descent (float)
                    totalDescent = BitUtility.getFloat(buffer, offset);
                    break;

                case 0x1017: // avg speed (float)
                    avgSpeed = BitUtility.getFloat(buffer, offset);
                    break;
            }

            if (id >= 0x2000) {
                // data section started or end of file marker
                break;
            }
        }

        fis.close();
    }

    /**
     * Gets the file name.
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets the beginning of the sport activity as a real time clock value
     * (milliseconds after Jan, 1st 1970 UTC).
     *
     * @return the timestamp of the beginning of the sport activity
     */
    public long getStartTimestampRtc() {
        return startTimestampRtc;
    }

    /**
     * Gets the end of the sport activity as a real time clock value
     * (milliseconds after Jan, 1st 1970 UTC).
     *
     * @return the timestamp fo the end of the sport activity
     */
    public long getStopTimestampRtc() {
        return stopTimestampRtc;
    }

    /**
     * Gets an abstract beginning of the sport activity in nanoseconds.
     * This value can be compared to sensor event timestamps.
     *
     * @return the beginning of the sport activity in nanoseconds
     */
    public long getStartNanoseconds() {
        return startNanoseconds;
    }

    /**
     * Gets an abstract end of the sport activity in nanoseconds.
     * This value can be compared to sensor event timestamps.
     *
     * @return the end of the sport activity in nanoseconds
     */
    public long getStopNanoseconds() {
        return stopNanoseconds;
    }

    /**
     * Gets the file size in bytes.
     *
     * @return the file size in bytes
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Gets an average heart rate in beats per minute.
     *
     * @return the average heart rate in beats per minute
     */
    public float getAvgHeartRate() {
        return avgHeartRate;
    }

    /**
     * Gets a maximum recorded heart rate in beats per minute.
     *
     * @return the maximum recorded heart rate in beats per minute
     */
    public int getMaxHeartRate() {
        return maxHeartRate;
    }

    /**
     * Gets a total number of steps recorded within the sport activity.
     *
     * @return the total number of steps within the sport activity
     */
    public int getTotalSteps() {
        return totalSteps;
    }

    /**
     * Gets an average step rate in steps per minute.
     *
     * @return the average step rate in steps per minute
     */
    public float getAvgStepRate() {
        return avgStepRate;
    }

    /**
     * Gets total recorded ascent in meters.
     *
     * @return the total recorded ascent in meters
     */
    public float getTotalAscent() {
        return totalAscent;
    }

    /**
     * Gets total recorded descent in meters.
     *
     * @return the total recorded descent in meters
     */
    public float getTotalDescent() {
        return totalDescent;
    }

    /**
     * Gets an average speed in kilometers per hour.
     *
     * @return the average speed in kilometers per hour
     */
    public float getAvgSpeed() {
        return avgSpeed;
    }
}
