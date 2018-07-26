package feign.reactive.client.statushandler;

import feign.reactive.client.ReactiveHttpResponse;
import reactor.core.publisher.Mono;

/**
 * @author Sergii Karpenko
 */
public interface ReactiveStatusHandler {

	boolean shouldHandle(int status);

	Mono<? extends Throwable> decode(String methodKey, ReactiveHttpResponse<?> response);
}