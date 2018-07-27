package feign;

import static feign.Util.isDefault;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Logger.Level;
import feign.Request.Options;
import feign.Target.HardCodedTarget;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class ReactiveFeign {

  private final ParseHandlersByName targetToHandlersByName;
  private final InvocationHandlerFactory invocationHandlerFactory;

  public ReactiveFeign(ParseHandlersByName targetToHandlersByName,
      InvocationHandlerFactory invocationHandlerFactory) {
    this.targetToHandlersByName = targetToHandlersByName;
    this.invocationHandlerFactory = invocationHandlerFactory;
  }

  public static Builder builder() {
    return new Builder();
  }

  public <T> T newInstance(Target<T> target) {
    final Map<String, MethodHandler> nameToHandler = targetToHandlersByName
        .apply(target);
    final Map<Method, MethodHandler> methodToHandler = new LinkedHashMap<>();
    final List<DefaultMethodHandler> defaultMethodHandlers = new LinkedList<>();

    for (final Method method : target.type().getMethods()) {
      if (isDefault(method)) {
        final DefaultMethodHandler handler = new DefaultMethodHandler(method);
        defaultMethodHandlers.add(handler);
        methodToHandler.put(method, handler);
      } else {
        methodToHandler.put(method,
            nameToHandler.get(Feign.configKey(target.type(), method)));
      }
    }

    final InvocationHandler handler = invocationHandlerFactory.create(target, methodToHandler);
    T proxy = (T) Proxy.newProxyInstance(target.type().getClassLoader(),
        new Class<?>[] {target.type()}, handler);

    for (final DefaultMethodHandler defaultMethodHandler : defaultMethodHandlers) {
      defaultMethodHandler.bindTo(proxy);
    }

    return proxy;

  }

  public static class Builder {
    private Encoder encoder = new Encoder.Default();
    private Decoder decoder = new Decoder.Default();
    private Logger logger = new Logger.NoOpLogger();
    private Logger.Level logLevel = Level.NONE;
    private Retryer retryer = new Retryer.Default();
    private ErrorDecoder errorDecoder = new ErrorDecoder.Default();
    private Request.Options options = new Request.Options();
    private List<RequestInterceptor> requestInterceptors = new ArrayList<>();
    private Contract contract = new ReactiveContract(new Contract.Default());
    private ReactiveClient<?> client;
    private InvocationHandlerFactory invocationHandlerFactory =
        new ReactiveInvocationHandler.Factory();

    public Builder encoder(Encoder encoder) {
      this.encoder = encoder;
      return this;
    }

    public Builder decoder(Decoder decoder) {
      this.decoder = decoder;
      return this;
    }

    public Builder logger(Logger logger) {
      this.logger = logger;
      return this;
    }

    public Builder logLevel(Level logLevel) {
      this.logLevel = logLevel;
      return this;
    }

    public Builder retryer(Retryer retryer) {
      this.retryer = retryer;
      return this;
    }

    public Builder errorDecoder(ErrorDecoder errorDecoder) {
      this.errorDecoder = errorDecoder;
      return this;
    }

    public Builder options(Options options) {
      this.options = options;
      return this;
    }

    public Builder requestInterceptors(
        List<RequestInterceptor> requestInterceptors) {
      this.requestInterceptors = requestInterceptors;
      return this;
    }

    public Builder requestInterceptor(RequestInterceptor requestInterceptor) {
      this.requestInterceptors.add(requestInterceptor);
      return this;
    }

    public Builder client(ReactiveClient<?> client) {
      this.client = client;
      return this;
    }

    public Builder contract(Contract contract) {
      this.contract = new ReactiveContract(contract);
      return this;
    }

    public <T> T target(Class<T> apiType, String url) {
      return target(new HardCodedTarget<>(apiType, url));
    }

    public <T> T target(Target<T> target) {
      return build().newInstance(target);
    }

    public ReactiveFeign build() {
      final ParseHandlersByName handlersByName = new ParseHandlersByName(contract,
          buildReactiveMethodHandlerFactory());
      return new ReactiveFeign(handlersByName, invocationHandlerFactory);
    }

    protected ReactiveMethodHandlerFactory buildReactiveMethodHandlerFactory() {
      return new ReactiveClientMethodHandler.Factory();
    }
  }

  public static final class ParseHandlersByName {
    private final Contract contract;
    private final ReactiveMethodHandlerFactory factory;

    ParseHandlersByName(final Contract contract,
        final ReactiveMethodHandlerFactory factory) {
      this.contract = contract;
      this.factory = factory;
    }

    Map<String, MethodHandler> apply(final Target target) {
      final List<MethodMetadata> metadata = contract
          .parseAndValidatateMetadata(target.type());
      final Map<String, MethodHandler> result = new LinkedHashMap<>();

      for (final MethodMetadata md : metadata) {
        ReactiveMethodHandler methodHandler = factory.create(target, md);
        result.put(md.configKey(), methodHandler);
      }

      return result;
    }
  }

}
