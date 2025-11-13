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

import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.ecf.core.security.SSLContextFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public class TrustAllSSLContextFactory implements SSLContextFactory {
	private SSLContext trustAllContext;
	private ServiceRegistration<SSLContextFactory> registration;
	
	public TrustAllSSLContextFactory() {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_RANKING, 100);
		// Register a custom SSLContextFactory that trusts all certificates
		BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
		registration = context.registerService(SSLContextFactory.class, this, properties);
	}
	
	public void dispose() {
		registration.unregister();
	}

	@Override
	public SSLContext getDefault() throws NoSuchAlgorithmException {
		return createTrustAllSSLContext();
	}

	@Override
	public SSLContext getInstance(String protocol) throws NoSuchAlgorithmException {
		return createTrustAllSSLContext();
	}

	@Override
	public SSLContext getInstance(String protocol, String providerName) throws NoSuchAlgorithmException {
		return createTrustAllSSLContext();
	}

	private SSLContext createTrustAllSSLContext() throws NoSuchAlgorithmException {
		if (trustAllContext == null) {

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
				} }, null);
				trustAllContext = ctx;
			} catch (Exception e) {
				throw new NoSuchAlgorithmException("Failed to create trust-all SSL context", e);
			}
		}
		return trustAllContext;
	}
}
