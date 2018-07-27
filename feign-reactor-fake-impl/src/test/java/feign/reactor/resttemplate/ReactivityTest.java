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
package feign.reactor.resttemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import feign.reactor.ReactiveFeign;
import feign.reactor.resttemplate.client.RestTemplateFakeReactiveFeign;
import feign.reactor.testcase.IcecreamServiceApi;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Test;

public class ReactivityTest extends feign.reactor.ReactivityTest {

  @Override
  protected ReactiveFeign.Builder<IcecreamServiceApi> builder() {
    return RestTemplateFakeReactiveFeign.builder();
  }

  @Test(expected = ConditionTimeoutException.class)
  @Override
  public void shouldRunReactively() throws JsonProcessingException {
    super.shouldRunReactively();
  }
}
