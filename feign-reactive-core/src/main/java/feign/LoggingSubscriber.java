package feign;

import feign.Logger.Level;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class LoggingSubscriber<T> implements Subscriber<T> {

  private final Logger logger;
  private final Logger.Level level;

  public LoggingSubscriber(Logger logger, Level level) {
    this.logger = logger;
    this.level = level;
  }

  @Override
  public void onSubscribe(Subscription s) {

  }

  @Override
  public void onNext(T t) {

  }

  @Override
  public void onError(Throwable t) {

  }

  @Override
  public void onComplete() {

  }
}
