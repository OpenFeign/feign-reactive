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
package feign;

import static feign.Util.checkNotNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import feign.utils.MultiValueMapUtils;
import feign.utils.Pair;

/**
 * Method handler for asynchronous HTTP requests via {@link ReactiveClient}.
 *
 * @author Sergii Karpenko
 */
public class ReactiveClientMethodHandler implements ReactiveMethodHandler {

  private static final Pattern PATTERN = Pattern.compile("\\{([^}]+)\\}");

  private final Target target;
  private final MethodMetadata methodMetadata;
  private final ReactiveClient<?> client;
  private final Request.Options options;
  private final Function<Map<String, ?>, String> pathExpander;
  private final Map<String, List<Function<Map<String, ?>, String>>> headerExpanders;
  private final Map<String, Collection<String>> queriesAll;
  private final Map<String, List<Function<Map<String, ?>, String>>> queryExpanders;
  private final Type returnType;
  private final Logger logger;
  private final Logger.Level logLevel;

  private ReactiveClientMethodHandler(Target target,
      MethodMetadata methodMetadata,
      ReactiveClient<?> reactiveClient,
      Request.Options options,
      Logger logger,
      Logger.Level logLevel) {
    this.target = checkNotNull(target, "target must be not null");
    this.methodMetadata = checkNotNull(methodMetadata,
        "methodMetadata must be not null");
    this.client = checkNotNull(reactiveClient, "client must be not null");
    this.pathExpander = buildExpandFunction(methodMetadata.template().url());
    this.headerExpanders = buildExpanders(methodMetadata.template().headers());

    this.queriesAll = new HashMap<>(methodMetadata.template().queries());
    if (methodMetadata.formParams() != null) {
      methodMetadata.formParams()
          .forEach(param -> MultiValueMapUtils.add(queriesAll, param, "{" + param + "}"));
    }
    this.queryExpanders = buildExpanders(queriesAll);
    this.returnType = Types.getRawType(methodMetadata.returnType());
    this.options = options;
    this.logger = logger;
    this.logLevel = logLevel;
  }

  @Override
  @SuppressWarnings({"unchecked", "CallingSubscribeInNonBlockingScope"})
  public Publisher invoke(final Object[] argv) {
    final ReactiveRequest request = buildRequest(argv);
    Publisher<?> publisher = this.client.execute(request, options, returnType);

    /* hook into the publisher for logging */
    publisher.subscribe(new LoggingSubscriber<>(this.logger, this.logLevel));
    return publisher;
  }

  private ReactiveRequest buildRequest(Object[] argv) {
    Map<String, ?> substitutionsMap = methodMetadata.indexToName().entrySet().stream()
        .flatMap(e -> e.getValue().stream()
            .map(v -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), v)))
        .collect(Collectors.toMap(Map.Entry::getValue,
            entry -> argv[entry.getKey()]));

    try {
      String path = pathExpander.apply(substitutionsMap);
      Map<String, Collection<String>> queries = queries(argv, substitutionsMap);
      Map<String, List<String>> headers = headers(argv, substitutionsMap);

      URI uri = new URI(target.url() + path + queryLine(queries));

      return new ReactiveRequest(methodMetadata.template().method(), uri, headers, body(argv));

    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private String queryLine(Map<String, Collection<String>> queries) {
    if (queries.isEmpty()) {
      return "";
    }

    StringBuilder queryBuilder = new StringBuilder();
    for (Map.Entry<String, Collection<String>> query : queries.entrySet()) {
      String field = query.getKey();
      for (String value : query.getValue()) {
        queryBuilder.append('&');
        queryBuilder.append(field);
        if (value != null) {
          queryBuilder.append('=');
          if (!value.isEmpty()) {
            queryBuilder.append(value);
          }
        }
      }
    }
    queryBuilder.deleteCharAt(0);
    return queryBuilder.insert(0, '?').toString();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Collection<String>> queries(Object[] argv,
                                                    Map<String, ?> substitutionsMap) {
    Map<String, Collection<String>> queries = new LinkedHashMap<>();

    // queries from template
    queriesAll.keySet()
        .forEach(queryName -> MultiValueMapUtils.addAll(queries, queryName,
            queryExpanders.get(queryName).stream()
                .map(expander -> expander.apply(substitutionsMap))
                .collect(toList())));

    // queries from args
    if (methodMetadata.queryMapIndex() != null) {
      ((Map<String, ?>) argv[methodMetadata.queryMapIndex()])
          .forEach((key, value) -> {
            if (value instanceof Iterable) {
              ((Iterable<?>) value)
                  .forEach(element -> MultiValueMapUtils.add(queries, key, element.toString()));
            } else {
              MultiValueMapUtils.add(queries, key, value.toString());
            }
          });
    }

    return queries;
  }

  @SuppressWarnings("unchecked")
  private Map<String, List<String>> headers(Object[] argv, Map<String, ?> substitutionsMap) {

    Map<String, List<String>> headers = new LinkedHashMap<>();

    // headers from template
    methodMetadata.template().headers().keySet()
        .forEach(headerName -> MultiValueMapUtils.addAllOrdered(headers, headerName,
            headerExpanders.get(headerName).stream()
                .map(expander -> expander.apply(substitutionsMap))
                .collect(toList())));

    // headers from args
    if (methodMetadata.headerMapIndex() != null) {
      ((Map<String, ?>) argv[methodMetadata.headerMapIndex()])
          .forEach((key, value) -> {
            if (value instanceof Iterable) {
              ((Iterable<?>) value)
                  .forEach(
                      element -> MultiValueMapUtils.addOrdered(headers, key, element.toString()));
            } else {
              MultiValueMapUtils.addOrdered(headers, key, value.toString());
            }
          });
    }

    return headers;
  }

  @SuppressWarnings("unchecked")
  private Publisher<Object> body(Object[] argv) {
    if (methodMetadata.bodyIndex() != null) {
      Object body = argv[methodMetadata.bodyIndex()];
      if (body instanceof Publisher) {
        return (Publisher<Object>) body;
      } else {
        return Publishers.just(body);
      }
    } else {
      return Publishers.empty();
    }
  }

  private static Map<String, List<Function<Map<String, ?>, String>>> buildExpanders(
      Map<String, Collection<String>> templates) {
    Stream<Pair<String, String>> headersFlattened = templates.entrySet().stream()
        .flatMap(e -> e.getValue().stream()
            .map(v -> new Pair<>(e.getKey(), v)));
    return headersFlattened.collect(groupingBy(
        entry -> entry.left,
        mapping(entry -> buildExpandFunction(entry.right), toList())));
  }

  private static Function<Map<String, ?>, String> buildExpandFunction(String template) {
    List<Function<Map<String, ?>, String>> chunks = new ArrayList<>();
    Matcher matcher = PATTERN.matcher(template);
    int previousMatchEnd = 0;
    while (matcher.find()) {
      String textChunk = template.substring(previousMatchEnd, matcher.start());
      if (textChunk.length() > 0) {
        chunks.add(data -> textChunk);
      }

      String substitute = matcher.group(1);
      chunks.add(data -> {
        Object substitution = data.get(substitute);
        if (substitution != null) {
          return substitution.toString();
        } else {
          return substitute;
        }
      });
      previousMatchEnd = matcher.end();
    }

    String textChunk = template.substring(previousMatchEnd);
    if (textChunk.length() > 0) {
      chunks.add(data -> textChunk);
    }

    return traceData -> chunks.stream().map(chunk -> chunk.apply(traceData))
        .collect(Collectors.joining());
  }

  public static class Factory implements ReactiveMethodHandlerFactory {

    @Override
    public ReactiveClientMethodHandler create(Target target,
        final MethodMetadata metadata,
        final ReactiveClient<?> client,
        final Request.Options options) {
      return new ReactiveClientMethodHandler(target, metadata, client, options);
    }
  }

}
