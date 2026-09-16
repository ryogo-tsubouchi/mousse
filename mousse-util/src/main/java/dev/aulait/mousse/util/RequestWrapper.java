package dev.aulait.mousse.util;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** An HTTP request and its repeatable body, exposed to {@link RestClientFilter filters}. */
public final class RequestWrapper {
  private final HttpRequest request;
  private final byte[] body;

  public RequestWrapper(HttpRequest request) {
    this(request, new byte[0]);
  }

  public RequestWrapper(HttpRequest request, byte[] body) {
    this.request = Objects.requireNonNull(request);
    this.body = Objects.requireNonNull(body).clone();
  }

  public HttpRequest getRequest() {
    return request;
  }

  public String bodyAsString() {
    return new String(body, StandardCharsets.UTF_8);
  }
}
