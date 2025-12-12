/****************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.tests.filetransfer.httpclientjava.http2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.CharsetUtil;

/**
 * HTTP/2 server that can send GOAWAY frames on demand. This server is used to
 * test client behavior when receiving GOAWAY frames.
 */
public class Http2ServerWithGoaway {

	private final int port;
	private EventLoopGroup eventLoop;
	private Channel serverChannel;

	private AtomicInteger goawayAfterRequests = new AtomicInteger(-1);
	private AtomicBoolean goawayWasSend = new AtomicBoolean();
	private AtomicBoolean dataWasSend = new AtomicBoolean();

	public Http2ServerWithGoaway(int port) throws Exception {
		this.port = port;
		@SuppressWarnings("deprecation")
		SelfSignedCertificate ssc = new SelfSignedCertificate();
		SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
				.sslProvider(SslProvider.JDK)
				.ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
				.applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
						ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
						ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
						ApplicationProtocolNames.HTTP_2))
				.build();
		eventLoop = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

		ServerBootstrap b = new ServerBootstrap();
		b.group(eventLoop).channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						Http2ServerHandler handler = new Http2ServerHandler();
						ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
						ch.pipeline().addLast(new Http2ServerInitializer(handler));
					}
				});

		serverChannel = b.bind(port).sync().channel();
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
		if (eventLoop != null) {
			eventLoop.shutdownGracefully();
		}
	}

	/**
	 * Get the port the server is listening on
	 */
	public int getPort() {
		return port;
	}
	
	public void assertGoawayWasSend() {
		assertFalse(dataWasSend.get());
		assertTrue(goawayWasSend.compareAndSet(true, false));
	}
	
	public void reset() {
		goawayAfterRequests.set(-1);
		goawayWasSend.set(false);
		dataWasSend.set(false);
	}
	
	public void assertDataWasSend() {
		assertFalse(goawayWasSend.get());
		assertTrue(dataWasSend.compareAndSet(true, false));
	}

	private static final class Http2ServerInitializer extends ChannelInboundHandlerAdapter {

		private final Http2ServerHandler serverHandler;

		public Http2ServerInitializer(Http2ServerHandler serverHandler) {
			this.serverHandler = serverHandler;
		}

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			ChannelPipeline pipeline = ctx.pipeline();
			ctx.pipeline().addLast(
					Http2FrameCodecBuilder.forServer().initialSettings(Http2Settings.defaultSettings()).build());
			ctx.pipeline().addLast(serverHandler);
			pipeline.remove(this);
		}
	}

	private final class Http2ServerHandler extends ChannelInboundHandlerAdapter {

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			if (goawayAfterRequests.get() == 0) {
				sendGoaway(ctx);
			}
			super.channelActive(ctx);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof Http2HeadersFrame) {
				if (goawayAfterRequests.getAndDecrement() == 0) {
					sendGoaway(ctx);
				} else {
					Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;
					sendResponse(ctx, headersFrame);
				}
			} else {
				super.channelRead(ctx, msg);
			}
		}

		private void sendResponse(ChannelHandlerContext ctx, Http2HeadersFrame requestHeaders) {
			dataWasSend.set(true);
			DefaultHttp2Headers headers = new DefaultHttp2Headers();
			headers.status(HttpResponseStatus.OK.codeAsText());
			headers.set("content-type", "text/plain; charset=UTF-8");
			Http2HeadersFrame responseHeaders = new DefaultHttp2HeadersFrame(headers, false);
			responseHeaders.stream(requestHeaders.stream());
			ctx.write(responseHeaders);
			ByteBuf content = Unpooled.copiedBuffer("Hello from HTTP/2 server", CharsetUtil.UTF_8);
			DefaultHttp2DataFrame dataFrame = new DefaultHttp2DataFrame(content, true);
			dataFrame.stream(requestHeaders.stream());
			ctx.writeAndFlush(dataFrame);
		}

		private void sendGoaway(ChannelHandlerContext ctx) {
			goawayWasSend.set(true);
			DefaultHttp2GoAwayFrame goAwayFrame = new DefaultHttp2GoAwayFrame(Http2Error.NO_ERROR);
			goAwayFrame.setExtraStreamIds(0);
			ctx.writeAndFlush(goAwayFrame).addListener(future -> {
				if (future.isSuccess()) {
					ctx.close();
				}
			});
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			cause.printStackTrace();
			ctx.close();
		}
	}



}
