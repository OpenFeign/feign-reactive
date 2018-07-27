package feign;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Reference Publishers that provide basic support for our Core Classes.
 */
public final class Publishers {

  /**
   * An Empty Publisher.
   */
  public static Publisher<Object> empty() {
    return Subscriber::onComplete;
  }

  /**
   * A Simple Publisher that returns the provided object.
   *
   * @param data to be wrapped.
   * @return a Publisher that returns the object.
   */
  public static Publisher<Object> just(final Object data) {
    return s -> {
      s.onNext(data);
      s.onComplete();
    };
  }
}
