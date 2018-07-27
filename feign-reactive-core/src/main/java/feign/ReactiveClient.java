package feign;

import java.lang.reflect.Type;

import org.reactivestreams.Publisher;

import feign.Request.Options;

/**
 * Client Interface for Reactive Request Execution.
 * @param <T> type of object emitted by the {@link Publisher}
 */
public interface ReactiveClient<T> {

  /**
   * Executes the Request.
   *
   * @param request to execute.
   * @param options options to apply to this request.
   * @param returnType {@link Type} expected.
   * @return a {@link Publisher} of the desired type
   */
  Publisher<T> execute(
      ReactiveRequest request, Options options, Type returnType);

}
