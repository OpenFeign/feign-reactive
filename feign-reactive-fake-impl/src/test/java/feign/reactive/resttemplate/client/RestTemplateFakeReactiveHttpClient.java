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
package feign.reactive.resttemplate.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.MethodMetadata;
import feign.reactive.client.ReactiveHttpClient;
import feign.reactive.client.ReactiveHttpRequest;
import feign.reactive.client.ReactiveHttpResponse;
import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static feign.Util.resolveLastTypeParameter;
import static org.springframework.core.ParameterizedTypeReference.forType;

public class RestTemplateFakeReactiveHttpClient<T> implements ReactiveHttpClient<T>{

	private final RestTemplate restTemplate;
	private final boolean acceptGzip;
	private final Type returnPublisherType;
	private final ParameterizedTypeReference returnActualType;

	public RestTemplateFakeReactiveHttpClient(MethodMetadata methodMetadata, RestTemplate restTemplate,
											  boolean acceptGzip) {
		this.restTemplate = restTemplate;
		this.acceptGzip = acceptGzip;

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(mapper);
		restTemplate.getMessageConverters().add(0, converter);


		final Type returnType = methodMetadata.returnType();
		returnPublisherType = ((ParameterizedType) returnType).getRawType();
		returnActualType = forType(
				resolveLastTypeParameter(returnType, (Class<?>) returnPublisherType));
	}

	@Override
	public Mono<ReactiveHttpResponse<T>> executeRequest(ReactiveHttpRequest request) {

		Object body;
		if(request.body() instanceof Mono){
			body = ((Mono) request.body()).block();
		} else if(request.body() instanceof Flux){
			body = ((Flux<Object>) request.body()).collectList().block();
		} else {
			body = request.body();
		}

		ParameterizedTypeReference responseType;
		if(returnPublisherType == Mono.class){
			responseType = returnActualType;
		} else {
			responseType = forType(new ParameterizedType() {
				@Override
				public Type[] getActualTypeArguments() {
					return new Type[]{returnActualType.getType()};
				}

				@Override
				public Type getRawType() {
					return List.class;
				}

				@Override
				public Type getOwnerType() {
					return null;
				}
			});
		}

		try {

			MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(request.headers());
			if(acceptGzip){
				headers.add("Accept-Encoding", "gzip");
			}

			ResponseEntity<T> response = restTemplate.exchange(request.uri().toString(), HttpMethod.valueOf(request.method()),
					new HttpEntity(body, headers), responseType);

			return Mono.just(new ReactiveHttpResponse<T>() {
				@Override
				public int status() {
					return response.getStatusCodeValue();
				}

				@Override
				public Map<String, List<String>> headers() {
					return response.getHeaders();
				}

				@Override
				public Publisher<T> body() {
					if(returnPublisherType == Mono.class){
						return Mono.just(response.getBody());
					} else {
						return Flux.fromIterable((List<T>)response.getBody());
					}
				}

				@Override
				public Mono<byte[]> bodyData() {
					return Mono.just(new byte[0]);
				}
			});
		} catch (HttpClientErrorException ex)   {
			return Mono.just(new ReactiveHttpResponse<T>() {
				@Override
				public int status() {
					return ex.getStatusCode().value();
				}

				@Override
				public Map<String, List<String>> headers() {
					return ex.getResponseHeaders();
				}

				@Override
				public Publisher<T> body() {
					throw new UnsupportedOperationException();
				}

				@Override
				public Mono<byte[]> bodyData() {
					return Mono.just(ex.getResponseBodyAsByteArray());
				}
			});
		}

	}
}
