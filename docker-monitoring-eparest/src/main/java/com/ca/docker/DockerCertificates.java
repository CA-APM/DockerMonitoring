/*
 * Copyright (c) 2014 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ca.docker;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.net.ssl.SSLContext;

/**
 * DockerCertificates holds certificates for connecting to an HTTPS-secured Docker instance with
 * client/server authentication.
 */
public class DockerCertificates {
  public static final String DEFAULT_CA_CERT_NAME = "ca.pem";
  public static final String DEFAULT_CLIENT_CERT_NAME = "cert.pem";
  public static final String DEFAULT_CLIENT_KEY_NAME = "key.pem";


  private final SSLContext sslContext;
  private String path;

  
  public DockerCertificates(String cakey, String clientCertificate, String clientKey2, String keystorePassword) throws Exception {
    try {
      final CertificateFactory cf = CertificateFactory.getInstance("X.509");
      
      Path caCertPath = Paths.get(cakey);
      final Certificate caCert = cf.generateCertificate(Files.newInputStream(caCertPath));
      
      Path clientCertPath = Paths.get(clientCertificate);
      final Certificate clientCert = cf.generateCertificate(
          Files.newInputStream(clientCertPath));
      
      Path clientKeyPath = Paths.get(clientKey2);
      final PEMKeyPair clientKeyPair = (PEMKeyPair) new PEMParser(
          Files.newBufferedReader(clientKeyPath,
                                  Charset.defaultCharset()))
          .readObject();

      final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(
          clientKeyPair.getPrivateKeyInfo().getEncoded());
      final KeyFactory kf = KeyFactory.getInstance("RSA");
      final PrivateKey clientKey = kf.generatePrivate(spec);

      final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(null, null);
      trustStore.setEntry("ca", new KeyStore.TrustedCertificateEntry(caCert), null);

      final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      keyStore.setCertificateEntry("client", clientCert);
      
      keyStore.setKeyEntry("key", clientKey, keystorePassword.toCharArray(), new Certificate[]{clientCert});

      this.sslContext = SSLContexts.custom()
          .loadTrustMaterial(trustStore)
          .loadKeyMaterial(keyStore, keystorePassword.toCharArray())
          .useTLS()
          .build();

    } catch (
        Exception e) {
      throw new Exception(e);
    }
  }


public SSLContext sslContext() {
    return this.sslContext;
  }

  public X509HostnameVerifier hostnameVerifier() {
    return SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Path caCertPath;
    private Path clientKeyPath;
    private Path clientCertPath;

    public Builder dockerCertPath(final Path dockerCertPath) {
      this.caCertPath = dockerCertPath.resolve(DEFAULT_CA_CERT_NAME);
      this.clientKeyPath = dockerCertPath.resolve(DEFAULT_CLIENT_KEY_NAME);
      this.clientCertPath = dockerCertPath.resolve(DEFAULT_CLIENT_CERT_NAME);

      return this;
    }

    public Builder caCertPath(final Path caCertPath) {
      this.caCertPath = caCertPath;
      return this;
    }

    public Builder clientKeyPath(final Path clientKeyPath) {
      this.clientKeyPath = clientKeyPath;
      return this;
    }

    public Builder clientCertPath(final Path clientCertPath) {
      this.clientCertPath = clientCertPath;
      return this;
    }

    
  }
}
