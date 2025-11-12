/*******************************************************************************
 * Copyright (c) 2025 Christoph L�ubrich and others.
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
package org.eclipse.ecf.tests.filetransfer.httpclientjava.http2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * HTTP/2 server that can send GOAWAY frames on demand.
 * This server is used to test client behavior when receiving GOAWAY frames.
 */
public class Http2ServerWithGoaway {
    
    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final CountDownLatch startupLatch = new CountDownLatch(1);
    
	private AtomicInteger goawayAfterRequests = new AtomicInteger(-1);
    
    public Http2ServerWithGoaway(int port) {
        this.port = port;
    }
    
    /**
     * Start the HTTP/2 server with TLS and ALPN for HTTP/2 negotiation
     */
    @SuppressWarnings("deprecation")
    public void start() throws Exception {
        // Create SSL context for HTTP/2 with self-signed certificate
		SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
            .sslProvider(SslProvider.JDK)
            .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
            .applicationProtocolConfig(new ApplicationProtocolConfig(
                ApplicationProtocolConfig.Protocol.ALPN,
                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                ApplicationProtocolNames.HTTP_2))
            .build();
        
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) throws Exception {
                     Http2ServerHandler handler = new Http2ServerHandler(                   
                         goawayAfterRequests
                     );
                     // Add SSL handler for HTTPS
                     ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                     ch.pipeline().addLast(new Http2ServerInitializer(handler));
                 }
             });
            
            serverChannel = b.bind(port).sync().channel();
            startupLatch.countDown();
            System.out.println("HTTP/2 Server started on port " + port + " (TLS with ALPN for HTTP/2)");
        } catch (Exception e) {
            shutdown();
            throw e;
        }
    }
    
    /**
     * Wait for the server to start
     */
    public void awaitStartup() throws InterruptedException {
        startupLatch.await();
    }
    
    /**
     * Configure whether to send GOAWAY after N requests
     */
    public void setGoawayAfterRequests(int numRequests) {
		this.goawayAfterRequests.set(numRequests);
    }
    
    /**
     * Shutdown the server
     */
    public void shutdown() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
    
    /**
     * Get the port the server is listening on
     */
    public int getPort() {
        return port;
    }
    
    private static final class Http2ServerInitializer extends ChannelInboundHandlerAdapter {
        
        private final Http2ServerHandler serverHandler;
        
        public Http2ServerInitializer(Http2ServerHandler serverHandler) {
            this.serverHandler = serverHandler;
        }
        
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            ChannelPipeline pipeline = ctx.pipeline();
            
            System.out.println("Setting up HTTP/2 server pipeline with ALPN (TLS)");
            
            // Build HTTP/2 frame codec - ALPN negotiated HTTP/2 over TLS
            ctx.pipeline().addLast(Http2FrameCodecBuilder.forServer()
                .initialSettings(Http2Settings.defaultSettings())
                .build());
            
            // Add our custom handler
            ctx.pipeline().addLast(serverHandler);
            
            // Remove this initializer
            pipeline.remove(this);
        }
    }
    
    /**
     * Fallback handler for HTTP/1.1 requests that don't upgrade to HTTP/2
     */
    private static final class Http1FallbackHandler extends ChannelInboundHandlerAdapter {
        private final AtomicInteger goawayAfter;
        
        public Http1FallbackHandler(AtomicInteger goawayAfter) {
            this.goawayAfter = goawayAfter;
        }
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof FullHttpRequest) {
                System.out.println("Received HTTP/1.1 request (no upgrade): " + ((FullHttpRequest) msg).uri());
                FullHttpRequest request = (FullHttpRequest) msg;
                
                // Send HTTP/1.1 response
                ByteBuf content = Unpooled.copiedBuffer("Hello from HTTP/2 server (HTTP/1.1 fallback)", CharsetUtil.UTF_8);
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    content
                );
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                
                ctx.writeAndFlush(response).addListener(future -> {
                    if (goawayAfter.decrementAndGet() == 0) {
                        System.out.println("Request count reached, closing connection");
                    }
                    ctx.close();
                });
                
                request.release();
            } else {
                System.out.println("Passing through message of type: " + msg.getClass().getName());
                super.channelRead(ctx, msg);
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("Exception in HTTP/1.1 fallback handler: " + cause.getMessage());
            cause.printStackTrace();
            ctx.close();
        }
    }
    
    private static final class Http2ServerHandler extends ChannelInboundHandlerAdapter {
        
    	private AtomicInteger goawayAfter;
        
    	public Http2ServerHandler(AtomicInteger goawayAfter) {
    		this.goawayAfter = goawayAfter;
        }
        
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            if (goawayAfter.get()==0) {
                sendGoaway(ctx);
            }
            super.channelActive(ctx);
        }
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("Http2ServerHandler received message of type: " + msg.getClass().getName());
            if (msg instanceof Http2HeadersFrame) {
                Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;
    			sendResponse(ctx, headersFrame);
                if (goawayAfter.decrementAndGet() == 0) {
    				System.out.println("Request count reached , sending GOAWAY");
                    sendGoaway(ctx);
                }
            } else {
                System.out.println("Http2ServerHandler: Not an Http2HeadersFrame, passing through");
                super.channelRead(ctx, msg);
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
            System.out.println("Sending GOAWAY frame to client");
            DefaultHttp2GoAwayFrame goAwayFrame = new DefaultHttp2GoAwayFrame(Http2Error.NO_ERROR);
            goAwayFrame.setExtraStreamIds(0);
            ctx.writeAndFlush(goAwayFrame).addListener(future -> {
                if (future.isSuccess()) {
                    System.out.println("GOAWAY frame sent successfully");
                    // Close the connection after sending GOAWAY
                    ctx.close();
                } else {
                    System.err.println("Failed to send GOAWAY frame: " + future.cause());
                    future.cause().printStackTrace();
                }
            });
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("Exception in HTTP/2 handler: " + cause.getMessage());
            cause.printStackTrace();
            ctx.close();
        }
    }

}
