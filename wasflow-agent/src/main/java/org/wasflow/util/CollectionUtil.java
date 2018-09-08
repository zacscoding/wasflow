package org.wasflow.util;

import java.util.List;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class CollectionUtil {

    /**
     * 리스트가 비어있는지 체크하는 메소드
     */
    public static <T> boolean isEmpty(List<T> list) {
        return (list == null) || list.size() == 0;
    }

    /**
     * 리스트가 비어있지 않은지 체크하는 메소드
     */
    public static <T> boolean isNotEmpty(List<T> list) {
        return (list != null) && (list.size() > 0);
    }

    /**
     * 리스트의 사이즈를 구하는 메소드 (null일 경우 기본 값을 반환)
     */
    public static <T> int size(List<T> list, int defaultValue) {
        if (list == null) {
            return defaultValue;
        }

        return list.size();
    }

    /**
     * 리스트의 null 값을 제거하는 메소드
     */
    public static <T> void removeNullValue(List<T> list) {
        if (isNotEmpty(list)) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == null) {
                    list.remove(i);
                    i--;
                }
            }
        }
    }

    /**
     * 리스트의 null값을 제거하고, 남아있는 요소가 있는지 체크하는 메소드
     */
    public static <T> boolean removeNullValueAndGetExist(List<T> list) {
        removeNullValue(list);
        return isNotEmpty(list);
    }
}