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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2Settings;

/**
 * Initializer for HTTP/2 connection handling
 */
public class Http2ServerInitializer extends ChannelInboundHandlerAdapter {
    
    private final Http2ServerHandler serverHandler;
    
    public Http2ServerInitializer(Http2ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // Build HTTP/2 frame codec - using default connection
        ctx.pipeline().addLast(Http2FrameCodecBuilder.forServer()
            .initialSettings(Http2Settings.defaultSettings())
            .build());
        
        // Add our custom handler
        ctx.pipeline().addLast(serverHandler);
        
        // Remove this initializer as it's no longer needed
        ctx.pipeline().remove(this);
    }
}
