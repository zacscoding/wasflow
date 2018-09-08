package org.wasflow.agent.proxy;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.wasflow.agent.Agent;
import org.wasflow.agent.LOGGER;
import org.wasflow.util.BytesClassLoader;
import org.wasflow.util.FileUtil;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class LoaderManager {

    private static Map<Integer, ClassLoader> loaderMaps = new HashMap<Integer, ClassLoader>();

    /**
     * wasflow.http.jar를 통해 JarLoader를 생성
     *
     * @param parent HttpServletRequest의 클래스 로더
     */
    public static ClassLoader getHttpLoader(ClassLoader parent) {
        return createLoader(parent, "wasflow.http");
    }

    /**
     * ClassLoader를 생성하는 메소드
     *
     * @param parent 부모 클래스 로더
     * @param key    jar 이름
     *
     * @return parent를 부모 클래스 로더로하고, key(jar)의 바이트 클래스 로더를 반환한다.
     */
    private synchronized static ClassLoader createLoader(ClassLoader parent, String key) {
        /*  TEMP CODE :: etc 값 확인해야 됨  */
        int hashKey = (parent == null ? 0 : System.identityHashCode(parent));
        LOGGER.info("@@ [LoaderManager::createLoader()] hashKey : " + hashKey);
        /*  -- TEMP CODE :: etc 값 확인해야 됨  */
        ClassLoader loader = loaderMaps.get(hashKey);
        if (loader == null) {
            try {
                byte[] bytes = deployJarBytes(key);
                if (bytes != null) {
                    LOGGER.info("[LoaderManager::createLoader] byte is not null");
                    loader = new BytesClassLoader(bytes, parent);
                    loaderMaps.put(hashKey, loader);
                }
            } catch (Throwable t) {
                LOGGER.error("@@ [ERROR] LoaderManager::createLoader()", t);
            }
        }
        return loader;
    }

    /**
     * jar 파일 => byte를 얻는 메소드
     */
    private static byte[] deployJarBytes(String jarname) {
        try {
            InputStream is = Agent.class.getResourceAsStream("/" + jarname + ".jar");
            byte[] newBytes = FileUtil.readAll(is);
            is.close();

            if (LOGGER.isTrace()) {
                LOGGER.trace("@@ LoadJarBytes " + jarname + " " + len(newBytes) + " bytes");
            }

            return newBytes;
        } catch (Throwable t) {
            LOGGER.error("@@ fail to load jar bytes ", t);
            return null;
        }
    }

    private static int len(byte[] arr) {
        return arr == null ? 0 : arr.length;
    }
}