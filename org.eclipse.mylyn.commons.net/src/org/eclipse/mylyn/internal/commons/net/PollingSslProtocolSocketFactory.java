/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     BREDEX GmbH - fix for bug 295050
 *******************************************************************************/

package org.eclipse.mylyn.internal.commons.net;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.commons.core.net.NetUtil;
import org.eclipse.mylyn.commons.net.SslCertificateException;

/**
 * Provides support for managing SSL connections.
 * 
 * @author Nathan Hapke
 * @author Rob Elves
 * @author Steffen Pingel
 */
public class PollingSslProtocolSocketFactory implements SecureProtocolSocketFactory {

	private static final String KEY_STORE = "javax.net.ssl.keyStore"; //$NON-NLS-1$

	private static final String KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword"; //$NON-NLS-1$

	private static final String KEY_STORE_TYPE = "javax.net.ssl.keyStoreType"; //$NON-NLS-1$

	private final boolean hasKeyManager;

	private String keyStoreFileName;

	private String keyStorePassword;

	private String keyStoreType;

	private SSLSocketFactory socketFactory;

	public PollingSslProtocolSocketFactory() {
		KeyManager[] keymanagers = null;
		if (System.getProperty(KEY_STORE) != null && System.getProperty(KEY_STORE_PASSWORD) != null) {
			try {
				String type = System.getProperty(KEY_STORE_TYPE, KeyStore.getDefaultType());
				KeyStore keyStore = KeyStore.getInstance(type);
				char[] password = System.getProperty(KEY_STORE_PASSWORD).toCharArray();
				keyStore.load(new FileInputStream(System.getProperty(KEY_STORE)), password);
				KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				keyManagerFactory.init(keyStore, password);
				keymanagers = keyManagerFactory.getKeyManagers();
			} catch (Exception e) {
				CommonsNetPlugin.log(IStatus.ERROR, "Could not initialize keystore", e); //$NON-NLS-1$
			}
		}

		hasKeyManager = keymanagers != null;

		try {
			SSLContext sslContext = SSLContext.getInstance("SSL"); //$NON-NLS-1$
			sslContext.init(keymanagers, new TrustManager[] { new TrustAllTrustManager() }, null);
			this.socketFactory = sslContext.getSocketFactory();
		} catch (Exception e) {
			CommonsNetPlugin.log(IStatus.ERROR, "Could not initialize SSL context", e); //$NON-NLS-1$
		}
	}

	public PollingSslProtocolSocketFactory(String keyStoreFileName, String keyStorePassword, String keyStoreType) {
		this.keyStoreFileName = keyStoreFileName;
		this.keyStorePassword = keyStorePassword;
		this.keyStoreType = keyStoreType;
		this.hasKeyManager = false;
	}

	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
			UnknownHostException {
		return NetUtil.configureSocket(getSocketFactory().createSocket(socket, host, port, autoClose));
	}

	public Socket createSocket(String remoteHost, int remotePort) throws IOException, UnknownHostException {
		return NetUtil.configureSocket(getSocketFactory().createSocket(remoteHost, remotePort));
	}

	public Socket createSocket(String remoteHost, int remotePort, InetAddress clientHost, int clientPort)
			throws IOException, UnknownHostException {
		return NetUtil.configureSocket(getSocketFactory().createSocket(remoteHost, remotePort, clientHost, clientPort));
	}

	public Socket createSocket(String host, int port, InetAddress localAddress, int localPort,
			HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
		if (params == null) {
			throw new IllegalArgumentException("Parameters may not be null"); //$NON-NLS-1$
		}

		int timeout = params.getConnectionTimeout();
		final Socket socket = NetUtil.configureSocket(getSocketFactory().createSocket());
		socket.bind(new InetSocketAddress(localAddress, localPort));
		MonitoredRequest.connect(socket, new InetSocketAddress(host, port), timeout);
		return socket;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PollingSslProtocolSocketFactory other = (PollingSslProtocolSocketFactory) obj;
		if (keyStoreFileName == null) {
			if (other.keyStoreFileName != null) {
				return false;
			}
		} else if (!keyStoreFileName.equals(other.keyStoreFileName)) {
			return false;
		}
		if (keyStorePassword == null) {
			if (other.keyStorePassword != null) {
				return false;
			}
		} else if (!keyStorePassword.equals(other.keyStorePassword)) {
			return false;
		}
		if (keyStoreType == null) {
			if (other.keyStoreType != null) {
				return false;
			}
		} else if (!keyStoreType.equals(other.keyStoreType)) {
			return false;
		}
		return true;
	}

	public synchronized SSLSocketFactory getSocketFactory() throws IOException {
		if (socketFactory == null) {
			if (keyStoreFileName != null && keyStorePassword != null) {
				KeyManager[] keymanagers = null;
				try {
					if (keyStoreType == null) {
						try {
							keymanagers = openKeyStore(KeyStore.getDefaultType());
						} catch (Exception e) {
							keymanagers = openKeyStore("pkcs12"); //$NON-NLS-1$
						}
					} else {
						keymanagers = openKeyStore(keyStoreType);
					}
				} catch (Exception cause) {
					IOException e = new SslCertificateException();
					e.initCause(cause);
					throw e;
				}

				try {
					SSLContext sslContext = SSLContext.getInstance("SSL"); //$NON-NLS-1$
					sslContext.init(keymanagers, new TrustManager[] { new TrustAllTrustManager() }, null);
					this.socketFactory = sslContext.getSocketFactory();
				} catch (Exception cause) {
					IOException e = new SslCertificateException();
					e.initCause(cause);
					throw e;
				}
			}
			throw new IOException("Could not initialize SSL context"); //$NON-NLS-1$
		}
		return socketFactory;
	}

	private KeyManager[] openKeyStore(String type) throws KeyStoreException, IOException, NoSuchAlgorithmException,
			CertificateException, FileNotFoundException, UnrecoverableKeyException {
		KeyStore keyStore = KeyStore.getInstance(type);
		char[] password = keyStorePassword.toCharArray();
		keyStore.load(new FileInputStream(keyStoreFileName), password);
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, password);
		return keyManagerFactory.getKeyManagers();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((keyStoreFileName == null) ? 0 : keyStoreFileName.hashCode());
		result = prime * result + ((keyStorePassword == null) ? 0 : keyStorePassword.hashCode());
		result = prime * result + ((keyStoreType == null) ? 0 : keyStoreType.hashCode());
		return result;
	}

	/**
	 * Public for testing only.
	 */
	public boolean hasKeyManager() {
		return hasKeyManager;
	}

}
