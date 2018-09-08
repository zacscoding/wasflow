package org.wasflow.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class GsonUtil {

    private static Gson GSON = null;

    /**
     * JSON으로 변경하는 메소드
     */
    public static String toJson(Object inst) {
        if (GSON == null) {
            GSON = new GsonBuilder().disableHtmlEscaping().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).serializeNulls().create();
        }

        return GSON.toJson(inst);
    }

    /**
     * Object => JsonArray로 변경하는 메소드
     */
    public static String toJsonArray(Object inst) {
        if (GSON == null) {
            GSON = new GsonBuilder().disableHtmlEscaping().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).serializeNulls().create();
        }

        Object[] objs = new Object[1];
        objs[1] = inst;

        return GSON.toJson(objs);
    }

    public static String prettyString(Object inst) {
        return new GsonBuilder().disableHtmlEscaping().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).setPrettyPrinting().serializeNulls().create().toJson(inst);
    }
}