package feign.codec;

import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import java.io.IOException;
import java.lang.reflect.Type;

public class DelegatingReactiveDecoder implements Decoder {

  private Decoder delegate;

  public DelegatingReactiveDecoder(Decoder delegate) {
    this.delegate = delegate;
  }

  @Override
  public Object decode(Response response, Type type)
      throws IOException, DecodeException, FeignException {
    return null;
  }
}
