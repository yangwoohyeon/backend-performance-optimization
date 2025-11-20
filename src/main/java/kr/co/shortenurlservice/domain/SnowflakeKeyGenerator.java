package kr.co.shortenurlservice.domain;

import java.util.concurrent.atomic.AtomicLong;

public class SnowflakeKeyGenerator {
    private static final String BASE56_CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    private static final int BASE56 = BASE56_CHARACTERS.length();
    private static final long EPOCH = 1609459200000L; // Custom epoch (e.g., January 1, 2021)
    private static final int SERVER_ID_BITS = 5;
    private static final int SEQUENCE_BITS = 12;
    private static final int MAX_SEQUENCE = (1 << SEQUENCE_BITS) - 1;

    private static final int SERVER_ID = 1; // Static server ID, can be replaced with a more dynamic value if needed

    private static AtomicLong sequence = new AtomicLong(0);
    private static volatile long lastTimestamp = -1L;

    public static String generateSnowflakeKey() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        if (timestamp == lastTimestamp) {
            long currentSequence = sequence.incrementAndGet() & MAX_SEQUENCE;
            if (currentSequence == 0) {
                // Sequence overflow, wait till next millisecond
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence.set(0);
        }

        lastTimestamp = timestamp;

        long id = ((timestamp - EPOCH) << (SERVER_ID_BITS + SEQUENCE_BITS))
                | (SERVER_ID << SEQUENCE_BITS)
                | sequence.get();

        return encodeBase56(id);
    }

    private static long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    private static String encodeBase56(long id) {
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            int index = (int) (id % BASE56);
            sb.append(BASE56_CHARACTERS.charAt(index));
            id /= BASE56;
        }
        while (sb.length() < 8) {
            sb.append(BASE56_CHARACTERS.charAt(0));
        }
        return sb.reverse().toString();
    }
}
