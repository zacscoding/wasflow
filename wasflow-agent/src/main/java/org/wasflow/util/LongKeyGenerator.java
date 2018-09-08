package org.wasflow.util;

import java.util.Random;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class LongKeyGenerator {

    private static Random rand = new Random(System.currentTimeMillis());

    public static void setSeed(long seed) {
        rand.setSeed(seed);
    }

    public static long next() {
        return rand.nextLong();
    }
}