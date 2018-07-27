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
package feign.reactor.client;

import feign.MethodMetadata;
import org.apache.commons.httpclient.HttpStatus;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import java.util.function.BiFunction;

/**
 * Maps 404 error response to successful empty response
 *
 * @author Sergii Karpenko
 */
public class ResponseMappers {

  public static <T> BiFunction<MethodMetadata, ReactiveHttpResponse<T>, ReactiveHttpResponse<T>> ignore404() {
    return (MethodMetadata methodMetadata, ReactiveHttpResponse<T> response) -> {
      if (response.status() == HttpStatus.SC_NOT_FOUND) {
        return new DelegatingReactiveHttpResponse<T>(response) {
          @Override
          public int status() {
            return HttpStatus.SC_OK;
          }

          @Override
          public Publisher<T> body() {
            return Mono.empty();
          }
        };
      }
      return response;
    };
  }

  public static <T> ReactiveHttpClient<T> mapResponse(
                                                      ReactiveHttpClient<T> reactiveHttpClient,
                                                      MethodMetadata methodMetadata,
                                                      BiFunction<MethodMetadata, ReactiveHttpResponse<T>, ReactiveHttpResponse<T>> responseMapper) {
    return request -> reactiveHttpClient.executeRequest(request)
        .map(response -> responseMapper.apply(methodMetadata, response));
  }

}
