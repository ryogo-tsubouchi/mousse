package dev.aulait.mousse.util;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/** An HTTP request and its repeatable body, exposed to {@link RestClientFilter filters}. */
public final class RequestWrapper {
  private final HttpRequest request;
  private final byte[] body;

  public RequestWrapper(String url, Map<String, String> headers, String method) {
    this(url, headers, new byte[0], method);
  }

  public RequestWrapper(String url, Map<String, String> headers, Object body, String method) {
    this(url, headers, toBody(body), method);
  }

  public RequestWrapper(String url, Map<String, String> headers, byte[] body, String method) {
    this.body = Objects.requireNonNull(body).clone();
    HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
    headers.forEach(builder::header);
    this.request =
        "GET".equals(method) && this.body.length == 0
            ? builder.GET().build()
            : builder.method(method, BodyPublishers.ofByteArray(this.body)).build();
  }

  private static byte[] toBody(Object body) {
    if (body == null) {
      return new byte[0];
    }
    return JsonUtils.obj2str(body).getBytes(StandardCharsets.UTF_8);
  }

  public HttpRequest getRequest() {
    return request;
  }

  public String bodyAsString() {
    return new String(body, StandardCharsets.UTF_8);
  }
}
