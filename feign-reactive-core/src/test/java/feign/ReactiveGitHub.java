package feign;

import org.reactivestreams.Publisher;

public interface ReactiveGitHub {

  class Repository {
    String name;
  }

  class Contributor {
    String login;
  }

  @RequestLine("GET /users/{username}/repos?sort=full_name")
  Publisher<Repository> repos(@Param("username") String owner);

  @RequestLine("GET /repos/{owner}/{repo}/contributors")
  Publisher<Contributor> contributors(
      @Param("owner") String owner, @Param("repo") String repository);
}
