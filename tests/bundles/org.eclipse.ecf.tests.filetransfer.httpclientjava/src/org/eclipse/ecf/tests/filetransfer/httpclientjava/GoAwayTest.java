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
package org.eclipse.ecf.tests.filetransfer.httpclientjava;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.ecf.filetransfer.IFileTransferListener;
import org.eclipse.ecf.filetransfer.events.IFileTransferConnectStartEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveDoneEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveStartEvent;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.tests.filetransfer.AbstractRetrieveTestCase;
import org.eclipse.ecf.tests.filetransfer.httpclientjava.http2.Http2ServerWithGoaway;
import org.eclipse.ecf.tests.filetransfer.httpclientjava.http2.TrustAllSSLContextFactory;

public class GoAwayTest extends AbstractRetrieveTestCase {

	private File tmpFile;
	private Http2ServerWithGoaway server;
	private TrustAllSSLContextFactory sslContextFactory;

	protected void setUp() throws Exception {
		super.setUp();
		tmpFile = Files.createTempFile("ECFTest", "").toFile();
		sslContextFactory = new TrustAllSSLContextFactory();
		server = new Http2ServerWithGoaway(8433);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		tmpFile.delete();
		server.shutdown();
		sslContextFactory.dispose();
	}

	protected void handleStartConnectEvent(IFileTransferConnectStartEvent event) {
		super.handleStartConnectEvent(event);
	}

	protected void handleStartEvent(IIncomingFileTransferReceiveStartEvent event) {
		super.handleStartEvent(event);
		assertNotNull(event.getFileID());
		assertNotNull(event.getFileID().getFilename());
		Map<?, ?> responseHeaders = event.getResponseHeaders();
		assertNotNull(responseHeaders);
		trace("responseHeaders=" + responseHeaders);
		try {
			incomingFileTransfer = event.receive(tmpFile);
		} catch (final IOException e) {
			fail(e.getLocalizedMessage());
		}
	}

	public void testOk() throws Exception {
		server.reset();
		receiveFile();
		server.assertDataWasSend();
		onErrorThrow();
	}

	public void testImmediatetGoaway() throws Exception {
		server.reset();
		server.setGoawayAfterRequests(0);
		receiveFile();
		server.assertGoawayWasSend();
		onErrorThrow();
	}

	public void testGoawayOnSecondRequest() throws Exception {
		server.reset();
		server.setGoawayAfterRequests(1);
		receiveFile();
		server.assertDataWasSend();
		onErrorThrow();
		done = false;
		doneEvents.clear();
		receiveFile();
		server.assertGoawayWasSend();
		onErrorThrow();
	}

	private void receiveFile() throws Exception {
		tmpFile.delete();
		final IFileTransferListener listener = createFileTransferListener();
		final IFileID fileID = createFileID(new URL("https://localhost:" + server.getPort() + "/test"));
		getRetrieveAdapter().sendRetrieveRequest(fileID, listener, null);
		waitForDone((int) TimeUnit.SECONDS.toMillis(10));
	}

	private void onErrorThrow() throws Exception {
		IIncomingFileTransferReceiveDoneEvent doneEvent = getDoneEvent();
		Exception exception = doneEvent.getException();
		if (exception != null) {
			throw new AssertionError(exception);
		}
	}
}
