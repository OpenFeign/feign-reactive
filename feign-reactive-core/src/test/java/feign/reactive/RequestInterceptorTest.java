package feign.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.FeignException;
import feign.reactive.testcase.IcecreamServiceApi;
import feign.reactive.testcase.domain.IceCreamOrder;
import feign.reactive.testcase.domain.OrderGenerator;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static feign.reactive.TestUtils.equalsComparingFieldByFieldRecursively;
import static feign.reactive.utils.MultiValueMapUtils.addOrdered;

/**
 * @author Sergii Karpenko
 */
abstract public class RequestInterceptorTest {

	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(
			wireMockConfig().dynamicPort());

	abstract protected ReactiveFeign.Builder<IcecreamServiceApi> builder();

	@Test
	public void shouldInterceptRequestAndSetAuthHeader() throws JsonProcessingException {

		String orderUrl = "/icecream/orders/1";

		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
		String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
						.withHeader("Accept", equalTo("application/json"))
						.willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)))
				.setPriority(100);

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.withHeader("Authorization", equalTo("Bearer mytoken123"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(orderStr)))
				.setPriority(1);

		IcecreamServiceApi clientWithoutAuth = builder()
				.target(IcecreamServiceApi.class,"http://localhost:" + wireMockRule.port());

		StepVerifier.create(clientWithoutAuth.findFirstOrder())
				.expectError(FeignException.class);

		IcecreamServiceApi clientWithAuth = builder()
				.requestInterceptor(request -> {
					addOrdered(request.headers(), "Authorization", "Bearer mytoken123");
					return request;
				})
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		StepVerifier.create(clientWithAuth.findFirstOrder())
				.expectNextMatches(equalsComparingFieldByFieldRecursively(orderGenerated))
				.expectComplete();
	}
}
