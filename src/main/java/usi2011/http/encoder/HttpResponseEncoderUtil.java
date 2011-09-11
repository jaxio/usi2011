/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package usi2011.http.encoder;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

/*
 * Code from this class comes from 
 * package org.jboss.netty.handler.codec.http;
 * ==> HttpResponseEncoder and its parent class HttpMessageEncoder
 */
public class HttpResponseEncoderUtil {
    public static String HTTP_1_1;
    public static String OK;
    public static String CREATED;
    public static byte[] HTTP_1_1_AS_BYTES;
    public static byte[] OK_AS_BYTES;
    public static byte[] CREATED_AS_BYTES;
    static {
        try {
            HTTP_1_1 = HttpVersion.HTTP_1_1.toString();
            HTTP_1_1_AS_BYTES = HttpVersion.HTTP_1_1.toString().getBytes("ASCII");
            OK = HttpVersion.HTTP_1_1.toString() + " " + String.valueOf(HttpResponseStatus.OK.getCode()) + " " + HttpResponseStatus.OK.getReasonPhrase() + "\r\n";
            OK_AS_BYTES = OK.getBytes("ASCII");
            CREATED = HttpVersion.HTTP_1_1.toString() + " " + String.valueOf(HttpResponseStatus.CREATED.getCode()) + " " +HttpResponseStatus.CREATED.getReasonPhrase() + "\r\n";
            CREATED_AS_BYTES = CREATED.getBytes("ASCII");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    static final byte SP = 32;
    /**
     * Colon ':'
     */
    static final byte COLON = 58;

    /**
     * Carriage return
     */
    static final byte CR = 13;

    /**
     * Line feed character
     */
    static final byte LF = 10;

    public static ChannelBuffer prepareRawResponse(ChannelHandlerContext ctx, Channel channel, HttpMessage msg) {
        ChannelBuffer header = ChannelBuffers.dynamicBuffer(channel.getConfig().getBufferFactory());
        encodeInitialLine(header, msg);
        encodeHeaders(header, msg);
        header.writeByte(CR);
        header.writeByte(LF);

        ChannelBuffer content = msg.getContent();
        if (!content.readable()) {
            return header; // no content
        } else {
            return wrappedBuffer(header, content);
        }
    }

    private static void encodeHeaders(ChannelBuffer buf, HttpMessage message) {
        try {
            for (Map.Entry<String, String> h : message.getHeaders()) {
                encodeHeader(buf, h.getKey(), h.getValue());
            }
        } catch (UnsupportedEncodingException e) {
            throw (Error) new Error().initCause(e);
        }
    }

    private static void encodeHeader(ChannelBuffer buf, String header, String value) throws UnsupportedEncodingException {
        buf.writeBytes(header.getBytes("ASCII"));
        buf.writeByte(COLON);
        buf.writeByte(SP);
        buf.writeBytes(value.getBytes("ASCII"));
        buf.writeByte(CR);
        buf.writeByte(LF);
    }

    public static void encodeInitialLine(ChannelBuffer buf, HttpMessage message) {
        try {
            final HttpResponse response = (HttpResponse) message;
            switch (response.getStatus().getCode()) {
            case 200:
                buf.writeBytes(OK_AS_BYTES);
                break;
            case 201:
                buf.writeBytes(CREATED_AS_BYTES);
                break;
            default:
                buf.writeBytes(HTTP_1_1_AS_BYTES);
                buf.writeByte(SP);
                buf.writeBytes(String.valueOf(response.getStatus().getCode()).getBytes("ASCII"));
                buf.writeByte(SP);
                buf.writeBytes(String.valueOf(response.getStatus().getReasonPhrase()).getBytes("ASCII"));
                buf.writeByte(CR);
                buf.writeByte(LF);
            }
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee);
        }
    }
}
