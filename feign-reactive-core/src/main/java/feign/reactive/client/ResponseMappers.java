package feign.reactive.client;

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
			BiFunction<MethodMetadata, ReactiveHttpResponse<T>, ReactiveHttpResponse<T>> responseMapper){
		return request -> reactiveHttpClient.executeRequest(request)
				.map(response -> responseMapper.apply(methodMetadata, response));
	}

}
