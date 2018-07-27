package feign;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.reactivestreams.Publisher;

/**
 * Contract that adds support {@link org.reactivestreams.Publisher} response types.
 */
public class ReactiveContract implements Contract {

  private Contract delegate;

  ReactiveContract(Contract delegate) {
    this.delegate = delegate;
  }

  @Override
  public List<MethodMetadata> parseAndValidatateMetadata(Class<?> targetType) {
    final List<MethodMetadata> methodsMetadata =
        this.delegate.parseAndValidatateMetadata(targetType);

    for (final MethodMetadata metadata : methodsMetadata) {
      final Type type = metadata.returnType();
      if (!isReactiveType(type)) {
        throw new IllegalArgumentException(String.format(
            "Method %s of contract %s do not return a Reactive Streams Compatible Type.",
            metadata.configKey(), targetType.getSimpleName()));
      }
    }

    return methodsMetadata;
  }

  protected boolean isReactiveType(Type type) {
    return (type instanceof ParameterizedType)
        && (((ParameterizedType) type).getRawType() == Publisher.class);
  }
}
