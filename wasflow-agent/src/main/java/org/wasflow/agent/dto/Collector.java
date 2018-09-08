package org.wasflow.agent.dto;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class Collector {

    private String ip;
    private int port;
    private String context;
    private boolean isMain;

    public Collector() {

    }

    public Collector(String ip, int port, String context, boolean isMain) {
        this.ip = ip;
        this.port = port;
        this.context = context;
        this.isMain = isMain;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public boolean isMain() {
        return isMain;
    }

    public void setMain(boolean main) {
        isMain = main;
    }
}