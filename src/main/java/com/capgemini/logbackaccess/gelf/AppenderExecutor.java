package com.capgemini.logbackaccess.gelf;

import ch.qos.logback.access.spi.AccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Converts a log event into a a payload or chunks and sends them to the graylog2-server
 */
public class AppenderExecutor {

    private final Transport transport;
    private final PayloadChunker payloadChunker;
    private final GelfConverter gelfConverter;
    private final Zipper zipper;
    private final int chunkThreshold;

    public AppenderExecutor(Transport transport,
                            PayloadChunker payloadChunker,
                            GelfConverter gelfConverter,
                            Zipper zipper,
                            int chunkThreshold) {
        this.transport = transport;
        this.payloadChunker = payloadChunker;
        this.gelfConverter = gelfConverter;
        this.zipper = zipper;
        this.chunkThreshold = chunkThreshold;
    }

    /**
     * The main append method. Takes the event that is being logged, formats if for GELF and then sends it over the wire
     * to the log server
     *
     * @param accessEvent The event that we are logging
     */
	public void append(final AccessEvent accessEvent) {

        byte[] payload = zipper.zip(gelfConverter.toGelf(accessEvent));

        // If we can fit all the information into one packet, then just send it
        if (payload.length < chunkThreshold) {
            transport.send(payload);

        // If the message is too long, then slice it up and send multiple packets
        } else {

            transport.send(payloadChunker.chunkIt(payload));
        }
    }
}
