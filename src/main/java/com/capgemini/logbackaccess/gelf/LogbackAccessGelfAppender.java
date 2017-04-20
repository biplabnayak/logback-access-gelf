package com.capgemini.logbackaccess.gelf;

import ch.qos.logback.access.spi.AccessEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;
import me.moocar.logbackgelf.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import static me.moocar.logbackgelf.util.InternetUtils.*;

/**
 * Responsible for Formatting a log event and sending it to a Graylog2 Server. Note that you can't swap in a different
 * Layout since the GELF format is static.
 */
public class LogbackAccessGelfAppender extends AppenderBase<AccessEvent> {

    // The following are configurable via logback configuration
    private String facility = "GELF";
    private String graylog2ServerHost = "localhost";
    private int graylog2ServerPort = 12201;
    private boolean useLoggerName = false;
    private boolean useMarker = false;
    private boolean useThreadName = false;
    private String graylog2ServerVersion = "0.9.6";
    private int chunkThreshold = 1000;
    private String messagePattern = "%m%rEx";
    private String shortMessagePattern = null;
    private Map<String, String> additionalFields = new HashMap<String, String>();
    private Map<String, String> staticAdditionalFields = new HashMap<String, String>();
    private boolean includeFullMDC = Boolean.TRUE;

    // The following are hidden (not configurable)
    private int shortMessageLength = 255;
    private static final int maxChunks = 127;
    private int messageIdLength = 8;
    private boolean padSeq = false;
    private final byte[] chunkedGelfId = new byte[]{0x1e, 0x0f};

    private AppenderExecutor appenderExecutor;

    //Newly added for Access log
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

            appenderExecutor.append(convertAccessEvent(accessEvent));

        } catch (RuntimeException e) {
            System.out.println(getStringStackTrace(e));
            this.addError("Error occurred: ", e);
            throw e;
        }
    }

    private ILoggingEvent convertAccessEvent(final AccessEvent accessEvent) {
        LoggingEvent loggingEvent = new LoggingEvent();
        loggingEvent.setLevel(Level.INFO);
        Map<String, String> mdcMap = getRequiredHeaders(accessEvent);
        populateAccessLogfields(mdcMap, accessEvent);
        loggingEvent.setMessage(accessEvent.getRequestURL() );
        loggingEvent.getMDCPropertyMap().putAll(mdcMap);
        return loggingEvent;
    }

    private Map<String, String> getRequiredHeaders(AccessEvent accessEvent) {
        Map<String, String> headerMap = new HashMap<>();
        if (headers != null && headers.length() > 0) {
            String [] arrHeaders = headers.split(",");
            for (int i = 0; i < arrHeaders.length; i++) {
                headerMap.put(arrHeaders[i], accessEvent.getRequestHeader(arrHeaders[i]));
            }
        }
        return headerMap;
    }

    private void populateAccessLogfields(Map<String, String> map, AccessEvent accessEvent) {
        if (checkBoolean(requestURI)) {
            map.put("_requestURI", accessEvent.getRequestURI());
        }
        if (checkBoolean(requestURL)) {
            map.put("_requestURL", accessEvent.getRequestURL());
        }
        if (checkBoolean(remoteHost)) {
            map.put("_remoteHost", accessEvent.getRemoteHost());
        }
        if (checkBoolean(remoteUser)) {
            map.put("_remoteUser", accessEvent.getRemoteUser());
        }
        if (checkBoolean(remoteAddr)) {
            map.put("_remoteAddr", accessEvent.getRemoteAddr());
        }
        if (checkBoolean(protocol)) {
            map.put("_protocol", accessEvent.getProtocol());
        }
        if (checkBoolean(method)) {
            map.put("_method", accessEvent.getMethod());
        }
        if (checkBoolean(serverName)) {
            map.put("_serverName", accessEvent.getServerName());
        }
        if (checkBoolean(requestContent)) {
            map.put("_requestContent", accessEvent.getRequestContent());
        }
        if (checkBoolean(responseContent)) {
            map.put("_responseContent", accessEvent.getResponseContent());
        }
        if (checkBoolean(statusCode)) {
            map.put("_statusCode", String.valueOf(accessEvent.getStatusCode()));
        }
        if (checkBoolean(contentLength)) {
            map.put("_contentLength", String.valueOf(accessEvent.getContentLength()));
        }
    }

    private boolean checkBoolean(Boolean val) {
        if (val == null) {
            return false;
        } else {
            return val.booleanValue();
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

            InetAddress address = getInetAddress(graylog2ServerHost);

            Transport transport = new Transport(graylog2ServerPort, address);

            if (graylog2ServerVersion.equals("0.9.5")) {
                messageIdLength = 32;
                padSeq = true;
            }

            String hostname = getLocalHostName();

            PayloadChunker payloadChunker = new PayloadChunker(chunkThreshold, maxChunks,
                    new MessageIdProvider(messageIdLength, MessageDigest.getInstance("MD5"), hostname),
                    new ChunkFactory(chunkedGelfId, padSeq));

            GelfConverter converter = new GelfConverter(facility, useLoggerName, useThreadName, useMarker, additionalFields,
                    staticAdditionalFields, shortMessageLength, hostname, messagePattern, shortMessagePattern,
                    includeFullMDC);

            appenderExecutor = new AppenderExecutor(transport, payloadChunker, converter, new Zipper(), chunkThreshold);

        } catch (Exception e) {

            throw new RuntimeException("Error initialising appender appenderExecutor", e);
        }
    }


    //////////// Logback Property Getter/Setters ////////////////

    /**
     * The name of your service. Appears in facility column in graylog2-web-interface
     */
    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    /**
     * The hostname of the graylog2 server to send messages to
     */
    public String getGraylog2ServerHost() {
        return graylog2ServerHost;
    }

    public void setGraylog2ServerHost(String graylog2ServerHost) {
        this.graylog2ServerHost = graylog2ServerHost;
    }

    /**
     * The port of the graylog2 server to send messages to
     */
    public int getGraylog2ServerPort() {
        return graylog2ServerPort;
    }

    public void setGraylog2ServerPort(int graylog2ServerPort) {
        this.graylog2ServerPort = graylog2ServerPort;
    }

    /**
     * If true, an additional field call "_loggerName" will be added to each gelf message. Its contents will be the
     * fully qualified name of the logger. e.g: com.company.Thingo.
     */
    public boolean isUseLoggerName() {
        return useLoggerName;
    }

    public void setUseLoggerName(boolean useLoggerName) {
        this.useLoggerName = useLoggerName;
    }

    public boolean isUseMarker() {
        return useMarker;
    }

    public void setUseMarker(boolean useMarker) {
        this.useMarker = useMarker;
    }

    /**
     * If true, an additional field call "_threadName" will be added to each gelf message. Its contents will be the
     * Name of the thread. Defaults to "false".
     */
    public boolean isUseThreadName() {
        return useThreadName;
    }

    public void setUseThreadName(boolean useThreadName) {
        this.useThreadName = useThreadName;
    }

    /**
     * additional fields to add to the gelf message. Here's how these work: <br/> Let's take an example. I want to log
     * the client's ip address of every request that comes into my web server. To do this, I add the ipaddress to the
     * slf4j MDC on each request as follows: <code> ... MDC.put("ipAddress", "44.556.345.657"); ... </code> Now, to
     * include the ip address in the gelf message, i just add the following to my logback.groovy: <code>
     * appender("GELF", GelfAppender) { ... additionalFields = [identity:"_identity"] ... } </code> in the
     * additionalFields map, the key is the name of the MDC to look up. the value is the name that should be given to
     * the key in the additional field in the gelf message.
     */
    public Map<String, String> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(Map<String, String> additionalFields) {
        this.additionalFields = additionalFields;
    }

    /**
     * static additional fields to add to every gelf message. Key is the additional field key (and should thus begin
     * with an underscore). The value is a static string.
     */
    public Map<String, String> getStaticAdditionalFields() {
        return staticAdditionalFields;
    }

    public void setStaticAdditionalFields(Map<String, String> staticAdditionalFields) {
        this.staticAdditionalFields = staticAdditionalFields;
    }

    /**
     * Add an additional field. This is mainly here for compatibility with logback.xml
     *
     * @param keyValue This must be in format key:value where key is the MDC key, and value is the GELF field
     *                 name. e.g "ipAddress:_ip_address"
     */
    public void addAdditionalField(String keyValue) {
        String[] splitted = keyValue.split(":");

        if (splitted.length != 2) {

            throw new IllegalArgumentException("additionalField must be of the format key:value, where key is the MDC "
                    + "key, and value is the GELF field name. But found '" + keyValue + "' instead.");
        }

        additionalFields.put(splitted[0], splitted[1]);
    }

    /**
     * Add a staticAdditional field. This is mainly here for compatibility with logback.xml
     *
     * @param keyValue This must be in format key:value where key is the additional field key, and value is a static
     *                 string. e.g "_node_name:www013"
     */
    public void addStaticAdditionalField(String keyValue) {
        String[] splitted = keyValue.split(":");

        if (splitted.length != 2) {

            throw new IllegalArgumentException("staticAdditionalField must be of the format key:value, where key is the "
                    + "additional field key (therefore should have a leading underscore), and value is a static string. " +
                    "e.g. _node_name:www013");
        }

        staticAdditionalFields.put(splitted[0], splitted[1]);
    }

    /**
     * The length of the message to truncate to
     */
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

    public String getMessagePattern() {
        return messagePattern;
    }

    public void setMessagePattern(String messagePattern) {
        this.messagePattern = messagePattern;
    }

    public String getShortMessagePattern() {
        return shortMessagePattern;
    }

    public void setShortMessagePattern(String shortMessagePattern) {
        this.shortMessagePattern = shortMessagePattern;
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
