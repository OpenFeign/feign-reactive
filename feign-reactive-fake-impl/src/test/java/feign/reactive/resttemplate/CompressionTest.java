package feign.reactive.resttemplate;

import feign.reactive.ReactiveFeign;
import feign.reactive.ReactiveOptions;
import feign.reactive.resttemplate.client.RestTemplateFakeReactiveFeign;
import feign.reactive.testcase.IcecreamServiceApi;

/**
 * @author Sergii Karpenko
 */
public class CompressionTest extends feign.reactive.CompressionTest {

	@Override
	protected ReactiveFeign.Builder<IcecreamServiceApi> builder(ReactiveOptions options) {
		return RestTemplateFakeReactiveFeign.builder(options);
	}
}
