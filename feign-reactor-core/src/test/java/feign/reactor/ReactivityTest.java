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
package feign.reactor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.reactor.testcase.IcecreamServiceApi;
import feign.reactor.testcase.domain.IceCreamOrder;
import feign.reactor.testcase.domain.OrderGenerator;
import org.awaitility.Duration;
import org.junit.ClassRule;
import org.junit.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.waitAtMost;

/**
 * @author Sergii Karpenko
 */
abstract public class ReactivityTest {

  public static final int DELAY_IN_MILLIS = 500;
  public static final int CALLS_NUMBER = 100;
  public static final int REACTIVE_GAIN_RATIO = 20;
  @ClassRule
  public static WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig()
          .asynchronousResponseEnabled(true)
          .dynamicPort());

  abstract protected ReactiveFeign.Builder<IcecreamServiceApi> builder();

  @Test
  public void shouldRunReactively() throws JsonProcessingException {

    IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
    String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(orderStr)
            .withFixedDelay(DELAY_IN_MILLIS)));

    IcecreamServiceApi client = builder()
        .target(IcecreamServiceApi.class,
            "http://localhost:" + wireMockRule.port());

    AtomicInteger counter = new AtomicInteger();

    new Thread(() -> {
      for (int i = 0; i < CALLS_NUMBER; i++) {
        client.findFirstOrder()
            .doOnNext(order -> counter.incrementAndGet())
            .subscribe();
      }
    }).start();

    int timeToCompleteReactively = CALLS_NUMBER * DELAY_IN_MILLIS / REACTIVE_GAIN_RATIO;
    waitAtMost(new Duration(timeToCompleteReactively, TimeUnit.MILLISECONDS))
        .until(() -> counter.get() == CALLS_NUMBER);
  }
}
