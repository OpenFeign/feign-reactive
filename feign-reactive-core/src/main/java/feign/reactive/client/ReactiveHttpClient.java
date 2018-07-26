/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feign.reactive.client;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;

/**
 * Client that execute http requests reactively
 *
 * @author Sergii Karpenko
 */
public interface ReactiveHttpClient<T> {

	Mono<ReactiveHttpResponse<T>> executeRequest(ReactiveHttpRequest request);

	default Publisher<T> executeRequest(ReactiveHttpRequest request, Type returnPublisherType) {
		Mono<ReactiveHttpResponse<T>> response = executeRequest(request);
		if (returnPublisherType == Mono.class) {
			return response.flatMap(resp -> (Mono<T>) resp.body());
		} else {
			return response.flatMapMany(resp -> resp.body());
		}
	}
}
