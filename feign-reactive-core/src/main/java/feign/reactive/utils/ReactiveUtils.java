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
package feign.reactive.utils;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import java.util.function.Consumer;

public class ReactiveUtils {

  public static <T> Subscriber<T> onNext(Consumer<T> consumer) {
    return new Subscriber<T>() {
      @Override
      public void onNext(T t) {
        consumer.accept(t);
      }

      @Override
      public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
      }

      @Override
      public void onError(Throwable throwable) {}

      @Override
      public void onComplete() {}
    };
  }
}
