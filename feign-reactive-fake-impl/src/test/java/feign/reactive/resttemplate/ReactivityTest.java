package feign.reactive.resttemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import feign.reactive.ReactiveFeign;
import feign.reactive.resttemplate.client.RestTemplateFakeReactiveFeign;
import feign.reactive.testcase.IcecreamServiceApi;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Test;

public class ReactivityTest extends feign.reactive.ReactivityTest {

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
