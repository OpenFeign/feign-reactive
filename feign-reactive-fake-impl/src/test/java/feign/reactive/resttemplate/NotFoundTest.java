package feign.reactive.resttemplate;

import feign.reactive.ReactiveFeign;
import feign.reactive.resttemplate.client.RestTemplateFakeReactiveFeign;
import feign.reactive.testcase.IcecreamServiceApi;

/**
 * @author Sergii Karpenko
 */
public class NotFoundTest extends feign.reactive.NotFoundTest{

	@Override
	protected ReactiveFeign.Builder<IcecreamServiceApi> builder() {
		return RestTemplateFakeReactiveFeign.builder();
	}
}
