/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.ecf.tests.http2.goaway;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.CharsetUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTTP/2 handler that processes requests and can send GOAWAY frames
 */
public class Http2ServerHandler extends ChannelInboundHandlerAdapter {
    
    private volatile boolean shouldSendGoaway = false;
    private volatile boolean sendGoawayImmediately = false;
    private volatile int goawayAfterRequests = -1;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private volatile ChannelHandlerContext savedContext = null;
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        savedContext = ctx;
        if (sendGoawayImmediately) {
            sendGoaway(ctx);
        }
        super.channelActive(ctx);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        savedContext = ctx;
        
        if (msg instanceof Http2HeadersFrame) {
            Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;
            
            // Increment request count
            int currentCount = requestCount.incrementAndGet();
            
            // Check if we should send GOAWAY after this request
            boolean sendGoawayNow = shouldSendGoaway || 
                                    (goawayAfterRequests > 0 && currentCount >= goawayAfterRequests);
            
            // Send a simple response
            if (!sendGoawayNow) {
                sendResponse(ctx, headersFrame);
            } else {
                // Send response first, then GOAWAY
                sendResponse(ctx, headersFrame);
                sendGoaway(ctx);
            }
        }
    }
    
    /**
     * Send a simple HTTP/2 response
     */
    private void sendResponse(ChannelHandlerContext ctx, Http2HeadersFrame requestHeaders) {
        // Create response headers
        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        headers.status(HttpResponseStatus.OK.codeAsText());
        headers.set("content-type", "text/plain; charset=UTF-8");
        
        // Send headers frame
        Http2HeadersFrame responseHeaders = new DefaultHttp2HeadersFrame(headers, false);
        responseHeaders.stream(requestHeaders.stream());
        ctx.write(responseHeaders);
        
        // Send data frame
        ByteBuf content = Unpooled.copiedBuffer("Hello from HTTP/2 server", CharsetUtil.UTF_8);
        DefaultHttp2DataFrame dataFrame = new DefaultHttp2DataFrame(content, true);
        dataFrame.stream(requestHeaders.stream());
        ctx.writeAndFlush(dataFrame);
    }
    
    /**
     * Send GOAWAY frame
     */
    private void sendGoaway(ChannelHandlerContext ctx) {
        System.out.println("Sending GOAWAY frame");
        DefaultHttp2GoAwayFrame goAwayFrame = new DefaultHttp2GoAwayFrame(Http2Error.NO_ERROR);
        goAwayFrame.setExtraStreamIds(0);
        ctx.writeAndFlush(goAwayFrame).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("GOAWAY frame sent successfully");
                // Close the connection after sending GOAWAY
                ctx.close();
            } else {
                System.err.println("Failed to send GOAWAY frame: " + future.cause());
            }
        });
    }
    
    /**
     * Trigger GOAWAY on next request or immediately if connection exists
     */
    public void triggerGoaway() {
        shouldSendGoaway = true;
        if (savedContext != null && savedContext.channel().isActive()) {
            sendGoaway(savedContext);
        }
    }
    
    /**
     * Configure whether to send GOAWAY immediately on connection
     */
    public void setSendGoawayImmediately(boolean immediate) {
        this.sendGoawayImmediately = immediate;
    }
    
    /**
     * Configure whether to send GOAWAY after N requests
     */
    public void setGoawayAfterRequests(int numRequests) {
        this.goawayAfterRequests = numRequests;
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
