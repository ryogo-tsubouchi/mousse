package dev.aulait.mousse.util;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/** Logs response details around a {@link RestClient} HTTP call. */
@Slf4j
public class ResponseLoggingFilter implements RestClientFilter {

  @Override
  public <T> HttpResponse<T> filter(
      RestClientRequest request, HttpResponse.BodyHandler<T> bodyHandler, FilterContext context) {
    HttpResponse<T> response = context.next(request, bodyHandler);
    log.info("Response status: {}", response.statusCode());
    log.info("Response headers: {}", response.headers().map());
    if (log.isDebugEnabled()) {
      log.debug("Response body: {}", bodyAsString(response.body()));
    }
    return response;
  }

  private String bodyAsString(Object body) {
    if (body instanceof byte[] bytes) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    return Objects.toString(body, "");
  }
}
