package feign;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

@RunWith(MockitoJUnitRunner.class)
public class ReactiveFeignTest {

  @Mock
  private ReactiveClient<String> client;

  public void canCreateTarget() {
    ReactiveGitHub gitHub = ReactiveFeign.builder()
        .client(this.client)
        .target(ReactiveGitHub.class, "https://api.github.com");
  }

}
