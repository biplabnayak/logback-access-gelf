package com.capgemini.logbackaccess.gelf;

import ch.qos.logback.access.spi.AccessEvent;
import ch.qos.logback.core.AppenderBase;
import com.capgemini.logbackaccess.gelf.util.InternetUtils;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for Formatting a log event and sending it to a Graylog2 Server.
 */
public class AccessLogGelfAppender extends AppenderBase<AccessEvent> {

    // The following are configurable via logback configuration
    private String facility = "GELF";
    private String graylog2ServerHost = "localhost";
    private int graylog2ServerPort = 12201;
    private String graylog2ServerVersion = "0.9.6";
    private int chunkThreshold = 1000;
    private Map<String, String> staticAdditionalFields = new HashMap<String, String>();

    // The following are hidden (not configurable)
    private int shortMessageLength = 255;
    private static final int maxChunks = 127;
    private int messageIdLength = 8;
    private boolean padSeq = false;
    private final byte[] chunkedGelfId = new byte[]{0x1e, 0x0f};

    private AppenderExecutor appenderExecutor;

    // Added
    private String headers;
    private Boolean requestURI;
    private Boolean requestURL;
    private Boolean remoteHost;
    private Boolean remoteUser;
    private Boolean remoteAddr;
    private Boolean protocol;
    private Boolean method;
    private Boolean serverName;
    private Boolean requestContent;
    private Boolean responseContent;
    private Boolean statusCode;
    private Boolean contentLength;

    /**
     * The main append method. Takes the event that is being logged, formats if for GELF and then sends it over the wire
     * to the log server
     *
     * @param accessEvent The event that we are logging
     */
    @Override
    protected void append(AccessEvent accessEvent) {

        try {

            appenderExecutor.append(accessEvent);

        } catch (RuntimeException e) {
            System.out.println(getStringStackTrace(e));
            this.addError("Error occurred: ", e);
            throw e;
        }
    }

    private String getStringStackTrace(Exception e) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        return result.toString();
    }

    @Override
    public void start() {
        super.start();
        initExecutor();
    }

    /**
     * This is an ad-hoc dependency injection mechanism. We don't want create all these classes every time a message is
     * logged. They will hang around for the lifetime of the appender.
     */
    private void initExecutor() {

        try {

            InetAddress address = InternetUtils.getInetAddress(graylog2ServerHost);

            Transport transport = new Transport(graylog2ServerPort, address);

            if (graylog2ServerVersion.equals("0.9.5")) {
                messageIdLength = 32;
                padSeq = true;
            }

            String hostname = InternetUtils.getLocalHostName();

            PayloadChunker payloadChunker = new PayloadChunker(chunkThreshold, maxChunks,
                    new MessageIdProvider(messageIdLength, MessageDigest.getInstance("MD5"), hostname),
                    new ChunkFactory(chunkedGelfId, padSeq));

            GelfConverter converter = new GelfConverter(facility,
                    staticAdditionalFields,
                    hostname,
                    headers,
                    requestURI,
                    requestURL,
                    remoteHost,
                    remoteUser,
                    remoteAddr,
                    protocol,
                    method,
                    serverName,
                    requestContent,
                    responseContent,
                    statusCode,
                    contentLength
                    );

            appenderExecutor = new AppenderExecutor(transport, payloadChunker, converter, new Zipper(), chunkThreshold);

        } catch (Exception e) {

            throw new RuntimeException("Error initialising appender appenderExecutor", e);
        }
    }


    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    public String getGraylog2ServerHost() {
        return graylog2ServerHost;
    }

    public void setGraylog2ServerHost(String graylog2ServerHost) {
        this.graylog2ServerHost = graylog2ServerHost;
    }

    public int getGraylog2ServerPort() {
        return graylog2ServerPort;
    }

    public void setGraylog2ServerPort(int graylog2ServerPort) {
        this.graylog2ServerPort = graylog2ServerPort;
    }

    public Map<String, String> getStaticAdditionalFields() {
        return staticAdditionalFields;
    }

    public void setStaticAdditionalFields(Map<String, String> staticAdditionalFields) {
        this.staticAdditionalFields = staticAdditionalFields;
    }

    public void addStaticAdditionalField(String keyValue) {
        String[] splitted = keyValue.split(":");

        if (splitted.length != 2) {

            throw new IllegalArgumentException("staticAdditionalField must be of the format key:value, where key is the "
                    + "additional field key (therefore should have a leading underscore), and value is a static string. " +
                    "e.g. _node_name:www013");
        }

        staticAdditionalFields.put(splitted[0], splitted[1]);
    }

    public int getShortMessageLength() {
        return shortMessageLength;
    }

    public void setShortMessageLength(int shortMessageLength) {
        this.shortMessageLength = shortMessageLength;
    }

    public String getGraylog2ServerVersion() {
        return graylog2ServerVersion;
    }

    public void setGraylog2ServerVersion(String graylog2ServerVersion) {
        this.graylog2ServerVersion = graylog2ServerVersion;
    }

    public int getChunkThreshold() {
        return chunkThreshold;
    }

    public void setChunkThreshold(int chunkThreshold) {
        this.chunkThreshold = chunkThreshold;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public Boolean getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(Boolean requestURI) {
        this.requestURI = requestURI;
    }

    public Boolean getRequestURL() {
        return requestURL;
    }

    public void setRequestURL(Boolean requestURL) {
        this.requestURL = requestURL;
    }

    public Boolean getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(Boolean remoteHost) {
        this.remoteHost = remoteHost;
    }

    public Boolean getRemoteUser() {
        return remoteUser;
    }

    public void setRemoteUser(Boolean remoteUser) {
        this.remoteUser = remoteUser;
    }

    public Boolean getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(Boolean remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public Boolean getProtocol() {
        return protocol;
    }

    public void setProtocol(Boolean protocol) {
        this.protocol = protocol;
    }

    public Boolean getMethod() {
        return method;
    }

    public void setMethod(Boolean method) {
        this.method = method;
    }

    public Boolean getServerName() {
        return serverName;
    }

    public void setServerName(Boolean serverName) {
        this.serverName = serverName;
    }

    public Boolean getRequestContent() {
        return requestContent;
    }

    public void setRequestContent(Boolean requestContent) {
        this.requestContent = requestContent;
    }

    public Boolean getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(Boolean responseContent) {
        this.responseContent = responseContent;
    }

    public Boolean getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Boolean statusCode) {
        this.statusCode = statusCode;
    }

    public Boolean getContentLength() {
        return contentLength;
    }

    public void setContentLength(Boolean contentLength) {
        this.contentLength = contentLength;
    }
}
