package dev.aulait.mousse.util;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** An HTTP request and its repeatable body, exposed to {@link RestClientFilter filters}. */
public final class RestClientRequest {
  private final HttpRequest request;
  private final byte[] body;

  public RestClientRequest(HttpRequest request) {
    this(request, new byte[0]);
  }

  public RestClientRequest(HttpRequest request, byte[] body) {
    this.request = Objects.requireNonNull(request);
    this.body = Objects.requireNonNull(body).clone();
  }

  public HttpRequest request() {
    return request;
  }

  public String method() {
    return request.method();
  }

  public URI uri() {
    return request.uri();
  }

  public HttpHeaders headers() {
    return request.headers();
  }

  public String bodyAsString() {
    return new String(body, StandardCharsets.UTF_8);
  }
}
