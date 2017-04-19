package com.capgemini.logbackaccess.gelf;

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.access.spi.AccessEvent;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Responsible for formatting a log event into a GELF message
 */
public class GelfConverter {

    private final String facility;

    private final Map<String, String> staticAdditionalFields;
    private final String hostname;
    private final Gson gson;
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

    public GelfConverter(String facility,
                         Map<String, String> staticAdditionalFields,
                         String hostname,
                         String headers,
                         Boolean requestURI,
                         Boolean requestURL,
                         Boolean remoteHost,
                         Boolean remoteUser,
                         Boolean remoteAddr,
                         Boolean protocol,
                         Boolean method,
                         Boolean serverName,
                         Boolean requestContent,
                         Boolean responseContent,
                         Boolean statusCode,
                         Boolean contentLength
                        ) {
        this.facility = facility;
        this.staticAdditionalFields = staticAdditionalFields;
        this.hostname = hostname;
        this.headers = headers;
        this.requestURI = requestURI;
        this.requestURL = requestURL;
        this.remoteHost = remoteHost;
        this.remoteUser = remoteUser;
        this.remoteAddr = remoteAddr;
        this.protocol = protocol;
        this.method = method;
        this.serverName = serverName;
        this.requestContent = requestContent;
        this.responseContent = responseContent;
        this.statusCode = statusCode;
        this.contentLength = contentLength;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        this.gson = gsonBuilder.create();
    }

    /**
     * Converts a log event into GELF JSON.
     *
     * @param accessEvent The log event we're converting
     * @return The log event converted into GELF JSON
     */
    public String toGelf(AccessEvent accessEvent) {
        try {
            return gson.toJson(mapFields(accessEvent));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Error creating JSON message", e);
        }
    }

    /**
     * Creates a map of properties that represent the GELF message.
     *
     * @param accessEvent The log event
     * @return map of gelf properties
     */
    private Map<String, Object> mapFields(AccessEvent accessEvent) {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("facility", facility);

        map.put("host", hostname);

        // Ever since version 0.9.6, GELF accepts timestamps in decimal form.
        double logEventTimeTimeStamp = accessEvent.getTimeStamp() / 1000.0;

        map.put("timestamp", logEventTimeTimeStamp);

        map.put("version", "1.0");
        populateAccessLogfields(map, accessEvent);
        populateHeaders(map, accessEvent);
        staticAdditionalFields(map);

        return map;
    }

    private void populateHeaders(Map<String, Object> map, AccessEvent accessEvent) {
        if (headers != null && headers.length() >0) {
            String[] headerArr = headers.split(",");
            for (int i = 0; i < headerArr.length; i++) {
                String h = headerArr[i];
                map.put("_header" + h, accessEvent.getRequestHeader(h));
            }
        }
    }

    private void populateAccessLogfields(Map<String, Object> map, AccessEvent accessEvent) {
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
            map.put("_statusCode", accessEvent.getStatusCode());
        }
        if (checkBoolean(contentLength)) {
            map.put("_contentLength", accessEvent.getContentLength());
        }
    }

    private boolean checkBoolean(Boolean val) {
        if (val == null) {
            return false;
        } else {
            return val.booleanValue();
        }
    }


    private void staticAdditionalFields(Map<String,Object> map) {

        for (String key : staticAdditionalFields.keySet()) {
            map.put(key, (staticAdditionalFields.get(key)));
        }
    }

}
