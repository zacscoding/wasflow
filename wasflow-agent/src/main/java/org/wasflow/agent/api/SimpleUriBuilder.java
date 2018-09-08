package org.wasflow.agent.api;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class SimpleUriBuilder {

    private String protocol;
    private String host;
    private int port;
    private String context;
    private StringBuilder pathBuilder = new StringBuilder();

    private SimpleUriBuilder() {

    }

    public static SimpleUriBuilder build() {
        return new SimpleUriBuilder();
    }

    public static SimpleUriBuilder buildHttp() {
        return build().protocol("http://");
    }

    public SimpleUriBuilder protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public SimpleUriBuilder host(String host) {
        this.host = host;
        return this;
    }

    public SimpleUriBuilder port(int port) {
        this.port = port;
        return this;
    }

    public SimpleUriBuilder context(String context) {
        if (context == null || context.length() == 0) {
            context = "";
        } else {
            if (context.charAt(0) != '/') {
                context = "/" + context;
            }
        }

        this.context = context;
        return this;
    }

    public SimpleUriBuilder path(String path) {
        if (path == null || path.length() == 0) {
            path = "";
        } else {
            if (path.charAt(0) != '/') {
                pathBuilder.append('/');
            }
            pathBuilder.append(path);
        }

        return this;
    }

    public String create() {
        if (host == null || port <= 0) {
            return "";
        }

        if (protocol == null) {
            protocol = "http://";
        }

        String path = pathBuilder.toString();
        StringBuilder sb = new StringBuilder(protocol.length() + host.length() + context.length() + path.length() + 6);
        return sb.append(protocol).append(host).append(':').append(port).append(context).append(path).toString();
    }
}