package org.eclipse.ecf.tests.filetransfer.httpclientjava;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;

import org.eclipse.ecf.filetransfer.IFileTransferListener;
import org.eclipse.ecf.filetransfer.events.IFileTransferConnectStartEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveDataEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveStartEvent;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.tests.filetransfer.AbstractRetrieveTestCase;
import org.eclipse.ecf.tests.filetransfer.httpclientjava.http2.Http2ServerWithGoaway;

public class GoAwayTest extends AbstractRetrieveTestCase {
	
	File tmpFile = null;
	private Http2ServerWithGoaway server;

	protected void setUp() throws Exception {
		super.setUp();
		tmpFile = Files.createTempFile("ECFTest", "").toFile();
	    server = new Http2ServerWithGoaway(8433);
        // Start server in a separate thread
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        
        // Wait for server to start
        server.awaitStartup();
        Thread.sleep(1000); // Give server time to fully initialize
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		tmpFile.delete();
		server.shutdown();
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
		trace("responseHeaders="+responseHeaders);
		try {
			incomingFileTransfer = event.receive(tmpFile);
		} catch (final IOException e) {
			fail(e.getLocalizedMessage());
		}
	}

	public void testReceive() throws Exception {
		String url = "http://localhost:8433/test";
		final IFileTransferListener listener = createFileTransferListener();
		final IFileID fileID = createFileID(new URL(url));
		getRetrieveAdapter().sendRetrieveRequest(fileID, listener, null);

		waitForDone(360000);

		assertHasEvent(startEvents, IIncomingFileTransferReceiveStartEvent.class);
		assertHasMoreThanEventCount(dataEvents, IIncomingFileTransferReceiveDataEvent.class, 0);
		assertDoneOK();

		assertTrue(tmpFile.exists());
		assertTrue(tmpFile.length() > 0);
	}
}
