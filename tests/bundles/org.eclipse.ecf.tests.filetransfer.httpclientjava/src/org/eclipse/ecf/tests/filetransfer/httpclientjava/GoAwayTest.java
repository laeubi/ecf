package org.eclipse.ecf.tests.filetransfer.httpclientjava;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.ecf.core.security.SSLContextFactory;
import org.eclipse.ecf.filetransfer.IFileTransferListener;
import org.eclipse.ecf.filetransfer.events.IFileTransferConnectStartEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveDataEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveStartEvent;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.tests.filetransfer.AbstractRetrieveTestCase;
import org.eclipse.ecf.tests.filetransfer.httpclientjava.http2.Http2ServerWithGoaway;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public class GoAwayTest extends AbstractRetrieveTestCase {
	
	File tmpFile = null;
	private Http2ServerWithGoaway server;
	private ServiceRegistration<SSLContextFactory> sslContextFactoryRegistration;

	protected void setUp() throws Exception {
		super.setUp();
		tmpFile = Files.createTempFile("ECFTest", "").toFile();
		
		// Register a custom SSLContextFactory that trusts all certificates
		BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
		SSLContextFactory trustAllFactory = new SSLContextFactory() {
			private final SSLContext trustAllContext = createTrustAllSSLContext();
			
			@Override
			public SSLContext getDefault() throws NoSuchAlgorithmException {
				return trustAllContext;
			}
			
			@Override
			public SSLContext getInstance(String protocol) throws NoSuchAlgorithmException {
				return trustAllContext;
			}
			
			@Override
			public SSLContext getInstance(String protocol, String providerName) throws NoSuchAlgorithmException {
				return trustAllContext;
			}
			
			private SSLContext createTrustAllSSLContext() throws NoSuchAlgorithmException {
				try {
					SSLContext ctx = SSLContext.getInstance("TLS");
					ctx.init(null, new TrustManager[] { new X509TrustManager() {
						@Override
						public void checkClientTrusted(X509Certificate[] chain, String authType) {
							// Trust all
						}
						
						@Override
						public void checkServerTrusted(X509Certificate[] chain, String authType) {
							// Trust all
						}
						
						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return new X509Certificate[0];
						}
					}}, null);
					return ctx;
				} catch (Exception e) {
					throw new NoSuchAlgorithmException("Failed to create trust-all SSL context", e);
				}
			}
		};
		
		Dictionary<String, Object> properties = new Hashtable<>();
		sslContextFactoryRegistration = context.registerService(SSLContextFactory.class, trustAllFactory, properties);
		
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
		if (sslContextFactoryRegistration != null) {
			sslContextFactoryRegistration.unregister();
		}
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
		String url = "https://localhost:8433/test";
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
