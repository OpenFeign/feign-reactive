package feign.reactive.client;

public class ReadTimeoutException extends RuntimeException {

	public ReadTimeoutException(Throwable cause) {
		super(cause);
	}
}
