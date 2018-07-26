package feign.reactive.utils;

import feign.MethodMetadata;
import org.reactivestreams.Publisher;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static feign.Util.resolveLastTypeParameter;
import static java.util.Optional.ofNullable;

public class FeignUtils {

	public static String methodTag(MethodMetadata methodMetadata){
		return methodMetadata.configKey().substring(0,
				methodMetadata.configKey().indexOf('('));
	}

	public static Type returnPublisherType(MethodMetadata methodMetadata){
		final Type returnType = methodMetadata.returnType();
		return ((ParameterizedType) returnType).getRawType();
	}
}
