package no.nav.melosys.util;

import java.io.Serializable;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ULID implements Serializable, Comparable<ULID> {
    public static final int STR_LENGTH = 26;
    public static final int BIN_LENGTH = 16;
    public static final int ENTROPY_LENGTH = 10;
    public static final long MIN_TIME = 0L;
    public static final long MAX_TIME = 281474976710655L;
    private static final char[] C = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'};
    private static final byte[] V = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1, -1, 10, 11, 12, 13, 14, 15, 16, 17, 1, 18, 19, 1, 20, 21, 0, 22, 23, 24, 25, 26, -1, 27, 28, 29, 30, 31, -1, -1, -1, -1, -1, -1, 10, 11, 12, 13, 14, 15, 16, 17, 1, 18, 19, 1, 20, 21, 0, 22, 23, 24, 25, 26, -1, 27, 28, 29, 30, 31, -1, -1, -1, -1, -1};
    private final long msb;
    private final long lsb;

    public ULID(long msb, long lsb) {
        this.msb = msb;
        this.lsb = lsb;
    }

    public long getMsb() {
        return this.msb;
    }

    public long getLsb() {
        return this.lsb;
    }

    public UUID toUUID() {
        return new UUID(this.msb, this.lsb);
    }

    public String toString() {
        char[] chars = new char[]{C[(byte) ((int) ((this.msb >>> 56 & 255L) >>> 5 & 31L))], C[(byte) ((int) (this.msb >>> 56 & 31L))], C[(byte) ((int) ((this.msb >>> 48 & 255L) >>> 3))], C[(byte) ((int) (((this.msb >>> 48 & 255L) << 2 | (this.msb >>> 40 & 255L) >>> 6) & 31L))], C[(byte) ((int) ((this.msb >>> 40 & 255L) >>> 1 & 31L))], C[(byte) ((int) (((this.msb >>> 40 & 255L) << 4 | (this.msb >>> 32 & 255L) >>> 4) & 31L))], C[(byte) ((int) (((this.msb >>> 32 & 255L) << 1 | (this.msb >>> 24 & 255L) >>> 7) & 31L))], C[(byte) ((int) ((this.msb >>> 24 & 255L) >>> 2 & 31L))], C[(byte) ((int) (((this.msb >>> 24 & 255L) << 3 | (this.msb >>> 16 & 255L) >>> 5) & 31L))], C[(byte) ((int) (this.msb >>> 16 & 31L))], C[(byte) ((int) ((this.msb >>> 8 & 255L) >>> 3))], C[(byte) ((int) (((this.msb >>> 8 & 255L) << 2 | (this.msb & 255L) >>> 6) & 31L))], C[(byte) ((int) ((this.msb & 255L) >>> 1 & 31L))], C[(byte) ((int) (((this.msb & 255L) << 4 | (this.lsb >>> 56 & 255L) >>> 4) & 31L))], C[(byte) ((int) (((this.lsb >>> 56 & 255L) << 1 | (this.lsb >>> 48 & 255L) >>> 7) & 31L))], C[(byte) ((int) ((this.lsb >>> 48 & 255L) >>> 2 & 31L))], C[(byte) ((int) (((this.lsb >>> 48 & 255L) << 3 | (this.lsb >>> 40 & 255L) >>> 5) & 31L))], C[(byte) ((int) (this.lsb >>> 40 & 31L))], C[(byte) ((int) ((this.lsb >>> 32 & 255L) >>> 3))], C[(byte) ((int) (((this.lsb >>> 32 & 255L) << 2 | (this.lsb >>> 24 & 255L) >>> 6) & 31L))], C[(byte) ((int) ((this.lsb >>> 24 & 255L) >>> 1 & 31L))], C[(byte) ((int) (((this.lsb >>> 24 & 255L) << 4 | (this.lsb >>> 16 & 255L) >>> 4) & 31L))], C[(byte) ((int) (((this.lsb >>> 16 & 255L) << 1 | (this.lsb >>> 8 & 255L) >>> 7) & 31L))], C[(byte) ((int) ((this.lsb >>> 8 & 255L) >>> 2 & 31L))], C[(byte) ((int) (((this.lsb >>> 8 & 255L) << 3 | (this.lsb & 255L) >>> 5) & 31L))], C[(byte) ((int) (this.lsb & 31L))]};
        return new String(chars);
    }

    public int hashCode() {
        long hilo = this.msb ^ this.lsb;
        return (int) (hilo >> 32) ^ (int) hilo;
    }

    public boolean equals(Object obj) {
        if (null != obj && obj.getClass() == ULID.class) {
            ULID other = (ULID) obj;
            return this.msb == other.msb && this.lsb == other.lsb;
        } else {
            return false;
        }
    }

    public int compareTo(ULID val) {
        return this.msb < val.msb ? -1 : (this.msb > val.msb ? 1 : Long.compare(this.lsb, val.lsb));
    }

    public byte[] toBytes() {
        return new byte[]{(byte) ((int) (this.msb >> 56 & 255L)), (byte) ((int) (this.msb >> 48 & 255L)), (byte) ((int) (this.msb >> 40 & 255L)), (byte) ((int) (this.msb >> 32 & 255L)), (byte) ((int) (this.msb >> 24 & 255L)), (byte) ((int) (this.msb >> 16 & 255L)), (byte) ((int) (this.msb >> 8 & 255L)), (byte) ((int) (this.msb & 255L)), (byte) ((int) (this.lsb >> 56 & 255L)), (byte) ((int) (this.lsb >> 48 & 255L)), (byte) ((int) (this.lsb >> 40 & 255L)), (byte) ((int) (this.lsb >> 32 & 255L)), (byte) ((int) (this.lsb >> 24 & 255L)), (byte) ((int) (this.lsb >> 16 & 255L)), (byte) ((int) (this.lsb >> 8 & 255L)), (byte) ((int) (this.lsb & 255L))};
    }

    public long getTimestamp() {
        return this.msb >>> 16;
    }

    public byte[] getEntropy() {
        return new byte[]{(byte) ((int) (this.msb >> 8 & 255L)), (byte) ((int) (this.msb & 255L)), (byte) ((int) (this.lsb >> 56 & 255L)), (byte) ((int) (this.lsb >> 48 & 255L)), (byte) ((int) (this.lsb >> 40 & 255L)), (byte) ((int) (this.lsb >> 32 & 255L)), (byte) ((int) (this.lsb >> 24 & 255L)), (byte) ((int) (this.lsb >> 16 & 255L)), (byte) ((int) (this.lsb >> 8 & 255L)), (byte) ((int) (this.lsb & 255L))};
    }

    private static long bytesToLong(byte[] src, int offset) {
        return ((long) src[offset] & 255L) << 56 | ((long) src[offset + 1] & 255L) << 48 | ((long) src[offset + 2] & 255L) << 40 | ((long) src[offset + 3] & 255L) << 32 | ((long) src[offset + 4] & 255L) << 24 | ((long) src[offset + 5] & 255L) << 16 | ((long) src[offset + 6] & 255L) << 8 | (long) src[offset + 7] & 255L;
    }

    public static ULID random() {
        return random(ThreadLocalRandom.current());
    }

    public static ULID random(Random random) {
        byte[] entropy = new byte[10];
        random.nextBytes(entropy);
        return noCheckGenerate(System.currentTimeMillis(), entropy);
    }

    public static ULID generate(long time, byte[] entropy) {
        if (time >= 0L && time <= 281474976710655L) {
            if (entropy != null && entropy.length == 10) {
                return noCheckGenerate(time, entropy);
            } else {
                throw new IllegalArgumentException("Invalid entropy");
            }
        } else {
            throw new IllegalArgumentException("Invalid timestamp");
        }
    }

    private static ULID noCheckGenerate(long time, byte[] entropy) {
        long msb = time << 16 | (long) ((entropy[0] & 255) << 8) | (long) (entropy[1] & 255);
        long lsb = bytesToLong(entropy, 2);
        return new ULID(msb, lsb);
    }

    private static byte valOrFail(char c) {
        byte res;
        if (c <= 'z' && (res = V[c]) != -1) {
            return res;
        } else {
            throw new IllegalArgumentException("Invalid ULID char " + c);
        }
    }

    public static ULID fromString(String val) {
        if (val.length() != 26) {
            throw new IllegalArgumentException("Invalid ULID string");
        } else {
            byte[] in = new byte[]{valOrFail(val.charAt(0)), valOrFail(val.charAt(1)), valOrFail(val.charAt(2)), valOrFail(val.charAt(3)), valOrFail(val.charAt(4)), valOrFail(val.charAt(5)), valOrFail(val.charAt(6)), valOrFail(val.charAt(7)), valOrFail(val.charAt(8)), valOrFail(val.charAt(9)), valOrFail(val.charAt(10)), valOrFail(val.charAt(11)), valOrFail(val.charAt(12)), valOrFail(val.charAt(13)), valOrFail(val.charAt(14)), valOrFail(val.charAt(15)), valOrFail(val.charAt(16)), valOrFail(val.charAt(17)), valOrFail(val.charAt(18)), valOrFail(val.charAt(19)), valOrFail(val.charAt(20)), valOrFail(val.charAt(21)), valOrFail(val.charAt(22)), valOrFail(val.charAt(23)), valOrFail(val.charAt(24)), valOrFail(val.charAt(25))};
            long msb = (long) (in[0] << 5 | in[1]) << 56 | (long) (in[2] << 3 | (in[3] & 255) >>> 2) << 48 | (long) (in[3] << 6 | in[4] << 1 | (in[5] & 255) >>> 4) << 40 | (long) (in[5] << 4 | (in[6] & 255) >>> 1) << 32 | (long) (in[6] << 7 | in[7] << 2 | (in[8] & 255) >>> 3) << 24 | (long) (in[8] << 5 | in[9]) << 16 | (long) (in[10] << 3 | (in[11] & 255) >>> 2) << 8 | (long) (in[11] << 6 | in[12] << 1 | (in[13] & 255) >>> 4);
            long lsb = (long) (in[13] << 4 | (in[14] & 255) >>> 1) << 56 | (long) (in[14] << 7 | in[15] << 2 | (in[16] & 255) >>> 3) << 48 | (long) (in[16] << 5 | in[17]) << 40 | (long) (in[18] << 3 | (in[19] & 255) >>> 2) << 32 | (long) (in[19] << 6 | in[20] << 1 | (in[21] & 255) >>> 4) << 24 | (long) (in[21] << 4 | (in[22] & 255) >>> 1) << 16 | (long) (in[22] << 7 | in[23] << 2 | (in[24] & 255) >>> 3) << 8 | (long) (in[24] << 5 | in[25]);
            return new ULID(msb, lsb);
        }
    }

    public static ULID fromBytes(byte[] v) {
        if (v.length != 16) {
            throw new IllegalArgumentException("Invalid ULID bytes length: " + v.length);
        } else {
            long msb = bytesToLong(v, 0);
            long lsb = bytesToLong(v, 8);
            return new ULID(msb, lsb);
        }
    }

    public static ULID fromUUID(UUID val) {
        return new ULID(val.getMostSignificantBits(), val.getLeastSignificantBits());
    }
}
