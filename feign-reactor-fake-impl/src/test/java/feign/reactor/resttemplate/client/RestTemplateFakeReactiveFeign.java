/**
 * Copyright 2018 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package feign.reactor.resttemplate.client;

import feign.reactor.ReactiveFeign;
import feign.reactor.ReactiveOptions;
import feign.reactor.client.ReactiveHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * WebClient based implementation
 *
 * @author Sergii Karpenko
 */
public class RestTemplateFakeReactiveFeign {

  public static <T> ReactiveFeign.Builder<T> builder() {
    return builder(new RestTemplate(), false);
  }

  public static <T> ReactiveFeign.Builder<T> builder(ReactiveOptions options) {
    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory(
            HttpClientBuilder.create().build());
    if (options.getConnectTimeoutMillis() != null) {
      requestFactory.setConnectTimeout(options.getConnectTimeoutMillis());
    }
    if (options.getReadTimeoutMillis() != null) {
      requestFactory.setReadTimeout(options.getReadTimeoutMillis());
    }
    return builder(new RestTemplate(requestFactory),
        options.isTryUseCompression() != null && options.isTryUseCompression());
  }

  public static <T> ReactiveFeign.Builder<T> builder(RestTemplate restTemplate,
                                                     boolean acceptGzip) {
    return new ReactiveFeign.Builder<>(
        methodMetadata -> (ReactiveHttpClient<T>) new RestTemplateFakeReactiveHttpClient<>(
            methodMetadata, restTemplate, acceptGzip));
  }
}


