package feign.reactive.resttemplate;

import feign.reactive.ReactiveFeign;
import feign.reactive.ReactiveOptions;
import feign.reactive.resttemplate.client.RestTemplateFakeReactiveFeign;
import feign.reactive.testcase.IcecreamServiceApi;

/**
 * @author Sergii Karpenko
 */
public class DefaultMethodTest extends feign.reactive.DefaultMethodTest {

	@Override
	protected ReactiveFeign.Builder<IcecreamServiceApi> builder() {
		return RestTemplateFakeReactiveFeign.builder();
	}

	@Override
	protected <API> ReactiveFeign.Builder<API> builder(Class<API> apiClass) {
		return RestTemplateFakeReactiveFeign.builder();
	}

	@Override
	protected ReactiveFeign.Builder<IcecreamServiceApi> builder(ReactiveOptions options) {
		return RestTemplateFakeReactiveFeign.builder(options);
	}
}
