package org.wasflow.util;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class StringUtil {

    public static boolean isEmpty(String value) {
        return (value == null) || (value.length() == 0);
    }

    public static boolean isNotEmpty(String value) {
        return (value != null) && (value.length() > 0);
    }

    public static int countOccurrencesOf(String str, char ch) {
        if (str != null && str.length() != 0) {
            int count = 0;

            int idx;
            for (int pos = 0; (idx = str.indexOf(ch, pos)) != -1; pos = idx + 1) {
                ++count;
            }

            return count;
        } else {
            return 0;
        }
    }

    /**
     * class 이름을 . => /로 바꿔주는 메소드 e.g ) org.test.Sample => org/test/Sample
     */
    public static String parseClassName(Class<?> clazz) {
        return clazz == null ? "" : clazz.getName().replace('.', '/');
    }

    /**
     * 동적 배열 증가
     */
    public static String[] growStringArray(String[] params, int missingIdx) {
        if (params == null) {
            return null;
        }
        int length = Math.max(10, (int) (missingIdx * 1.2D));
        String[] newParams = new String[length];
        System.arraycopy(params, 0, newParams, 0, params.length);
        return newParams;
    }

    /**
     * String 배열의 전체 길이 합을 구하는 메소드
     * 만약 배열의 요소가 null 이면 defaultValue로 더한다.
     */
    public static int getLengthWithDefault(int defaultValue, String... vals) {
        if (vals == null) {
            return defaultValue;
        }
        int length = 0;

        for (String val : vals) {
            if (isEmpty(val)) {
                length += defaultValue;
            } else {
                length += val.length();
            }
        }

        return length;
    }

    /**
     * Object 의 toString()을 호출하는 메소드
     * inst가 null이면 "null"을 반환한다.
     */
    public static String toString(Object inst) {
        return toString(inst, "null");
    }

    /**
     * Object의 toString()을 호출하는 메소드
     * inst가 null이면 defaultValue를 반환한다.
     */
    public static String toString(Object inst, String defaultValue) {
        return inst == null ? defaultValue : inst.toString();
    }
}