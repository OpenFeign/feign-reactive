package feign.reactive.resttemplate;

import feign.reactive.ReactiveFeign;
import feign.reactive.resttemplate.client.RestTemplateFakeReactiveFeign;

/**
 * @author Sergii Karpenko
 */
public class ContractTest extends feign.reactive.ContractTest{

	@Override
	protected <T> ReactiveFeign.Builder<T> builder() {
		return RestTemplateFakeReactiveFeign.builder();
	}
}
