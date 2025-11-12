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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.util.concurrent.CountDownLatch;

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
    
    // Shared configuration across all handler instances
    private volatile boolean sendGoawayImmediately = false;
    private volatile int goawayAfterRequests = -1;
    
    public Http2ServerWithGoaway(int port) {
        this.port = port;
    }
    
    /**
     * Start the HTTP/2 server with SSL/TLS (required for HTTP/2)
     */
    public void start() throws Exception {
        // Create SSL context for HTTP/2
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
                     // Create handler with shared configuration
                     Http2ServerHandler handler = new Http2ServerHandler(
                         sendGoawayImmediately, 
                         goawayAfterRequests
                     );
                     ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                     ch.pipeline().addLast(new Http2ServerInitializer(handler));
                 }
             });
            
            serverChannel = b.bind(port).sync().channel();
            startupLatch.countDown();
            System.out.println("HTTP/2 Server started on port " + port);
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
}
