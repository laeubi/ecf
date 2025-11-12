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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case to reproduce HTTP/2 GOAWAY handling issues.
 * 
 * This test sets up an HTTP/2 server that can send GOAWAY frames and uses
 * Java 11's HttpClient to make requests. The test demonstrates the behavior
 * when a server sends GOAWAY frames.
 * 
 * Key findings:
 * - Java 11 HttpClient DOES handle GOAWAY gracefully by opening new connections
 * - This is actually correct and resilient behavior
 * - The p2 issue is likely about retry logic and error handling during long downloads
 */
public class Http2GoawayTest {
	static {
//		System.setProperty("jdk.httpclient.HttpClient.log", "all");
	}
    
    private Http2ServerWithGoaway server;
    private HttpClient httpClient;
    private static final int SERVER_PORT = 8443;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Create and start the HTTP/2 server
        server = new Http2ServerWithGoaway(SERVER_PORT);
        
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
        
        // Create HttpClient that accepts self-signed certificates and uses HTTP/2
        SSLContext sslContext = createTrustAllSSLContext();
        httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .sslContext(sslContext)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.shutdown();
        }
    }
    
    /**
     * Test normal HTTP/2 request without GOAWAY
     */
    @Test
    public void testNormalHttp2Request() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://localhost:" + SERVER_PORT + "/test"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertEquals("Hello from HTTP/2 server", response.body());
        System.out.println("✓ Normal request completed successfully");
    }
    
    /**
     * Test HTTP/2 GOAWAY after first request
     * This test demonstrates that Java HttpClient handles GOAWAY by creating new connections.
     * This is CORRECT behavior - the client is resilient to GOAWAY.
     */
    @Test
    public void testGoawayAfterFirstRequest() throws Exception {
        // Configure server to send GOAWAY after 1 request
        server.setGoawayAfterRequests(1);
        
        // First request should succeed
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://localhost:" + SERVER_PORT + "/test"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        System.out.println("✓ First request completed, server will send GOAWAY after this");
        
        // Wait a bit for GOAWAY to be processed
//        Thread.sleep(1000);
        
        // Second request - Java HttpClient will open a new connection
        // This demonstrates that Java HttpClient HANDLES GOAWAY correctly
        HttpRequest request2 = HttpRequest.newBuilder()
            .uri(URI.create("https://localhost:" + SERVER_PORT + "/test2"))
            .GET()
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response2 = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
        
        // With Java 11+ HttpClient, this succeeds by opening a new connection
        assertEquals(200, response2.statusCode());
        System.out.println("✓ Second request succeeded by opening new connection (GOAWAY handled correctly)");
    }
    
    /**
     * Test multiple requests where GOAWAY happens mid-stream
     * This simulates a scenario where several requests succeed but then GOAWAY is sent.
     * Java HttpClient handles this gracefully by creating new connections.
     */
    @Test
    public void testMultipleRequestsWithGoaway() throws Exception {
        // Configure server to send GOAWAY after 2 requests
        server.setGoawayAfterRequests(2);
        
        HttpRequest request1 = HttpRequest.newBuilder()
            .uri(URI.create("https://localhost:" + SERVER_PORT + "/request1"))
            .GET()
            .build();
        
        HttpResponse<String> response1 = httpClient.send(request1, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response1.statusCode());
        System.out.println("✓ Request 1 completed");
        
        HttpRequest request2 = HttpRequest.newBuilder()
            .uri(URI.create("https://localhost:" + SERVER_PORT + "/request2"))
            .GET()
            .build();
        
        HttpResponse<String> response2 = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response2.statusCode());
        System.out.println("✓ Request 2 completed, server will send GOAWAY after this");
        
        // Wait for GOAWAY to be processed
//        Thread.sleep(1000);
        
        // Third request - will succeed with new connection
        HttpRequest request3 = HttpRequest.newBuilder()
            .uri(URI.create("https://localhost:" + SERVER_PORT + "/request3"))
            .GET()
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response3 = httpClient.send(request3, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response3.statusCode());
        System.out.println("✓ Request 3 succeeded by opening new connection after GOAWAY");
    }
    
    /**
     * Test immediate GOAWAY on connection
     * Even with immediate GOAWAY, Java HttpClient retries with a new connection.
     */
    @Test
    public void testImmediateGoaway() throws Exception {
        // Configure server to send GOAWAY immediately
        server.setSendGoawayImmediately(true);
        
        // The HttpClient may retry with a new connection
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://localhost:" + SERVER_PORT + "/test"))
            .GET()
            .timeout(Duration.ofSeconds(5))
            .build();
        
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            // If it succeeds, it means HttpClient opened a new connection
            System.out.println("✓ Request succeeded despite immediate GOAWAY (new connection opened)");
            assertEquals(200, response.statusCode());
		}
    
    /**
     * Create an SSLContext that trusts all certificates (for testing with self-signed certs)
     */
    private SSLContext createTrustAllSSLContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
				public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                @Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                @Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }
}
