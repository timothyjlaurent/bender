/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright 2017 Nextdoor.com, Inc
 *
 */

package com.nextdoor.bender.ipc.es;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;

import com.nextdoor.bender.aws.auth.UrlSigningAuthConfig;
import com.nextdoor.bender.aws.auth.UserPassAuthConfig;
import com.nextdoor.bender.ipc.TransportSerializer;
import com.nextdoor.bender.ipc.http.AbstractHttpTransportFactory;

import vc.inreach.aws.request.AWSSigningRequestInterceptor;

public class ElasticSearchTransportFactory extends AbstractHttpTransportFactory {

  @Override
  protected String getPath() {
    ElasticSearchTransportConfig config = (ElasticSearchTransportConfig) super.config;
    return config.getBulkApiPath();
  }

  @Override
  protected TransportSerializer getSerializer() {
    ElasticSearchTransportConfig config = (ElasticSearchTransportConfig) super.config;

    return new ElasticSearchTransportSerializer(config.isUseHashId(), config.getDocumentType(),
        config.getIndex(), config.getIndexTimeFormat());
  }

  @Override
  protected CloseableHttpClient getClient(boolean useSSL, String url,
      Map<String, String> stringHeaders, int socketTimeout) {
    HttpClientBuilder cb = super.getClientBuilder(useSSL, url, stringHeaders, socketTimeout);
    ElasticSearchTransportConfig config = (ElasticSearchTransportConfig) super.config;

    if (config.getAuthConfig() != null) {
      if (config.getAuthConfig() instanceof UserPassAuthConfig) {
        cb = addUserPassAuth(cb, (UserPassAuthConfig) config.getAuthConfig());
      } else if (config.getAuthConfig() instanceof UrlSigningAuthConfig) {
        cb = addSigningAuth(cb, (UrlSigningAuthConfig) config.getAuthConfig());
      }
    }

    RequestConfig rc = RequestConfig.custom().setConnectTimeout(5000)
        .setSocketTimeout(config.getTimeout()).build();
    cb.setDefaultRequestConfig(rc);

    return cb.build();
  }

  private HttpClientBuilder addUserPassAuth(HttpClientBuilder cb, UserPassAuthConfig auth) {
    /*
     * Send auth via headers as the credentials provider method of auth does not work when using
     * SSL.
     */
    byte[] encodedAuth =
        Base64.encodeBase64((auth.getUsername() + ":" + auth.getPassword()).getBytes());
    String authHeader = "Basic " + new String(encodedAuth);

    cb.setDefaultHeaders(Arrays.asList(new BasicHeader(HttpHeaders.AUTHORIZATION, authHeader)));

    return cb;
  }

  private HttpClientBuilder addSigningAuth(HttpClientBuilder cb, UrlSigningAuthConfig auth) {
    return cb.addInterceptorLast(new AWSSigningRequestInterceptor(auth.getAWSSigner()));
  }
}
