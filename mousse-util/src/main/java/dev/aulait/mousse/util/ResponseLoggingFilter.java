package dev.aulait.mousse.util;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/** Logs response details around a {@link RestClient} HTTP call. */
@Slf4j
public class ResponseLoggingFilter implements RestClientFilter {

  @Override
  public <T> void filter(
      RequestWrapper request, ResponseWrapper<T> responseWrapper, FilterContext context) {
    context.next(request, responseWrapper);
    HttpResponse<T> response = responseWrapper.getResponse();
    log.info("Response status: {}", response.statusCode());
    log.info("Response headers: {}", response.headers().map());
    if (log.isDebugEnabled()) {
      log.debug("Response body: {}", bodyAsString(response.body()));
    }
  }

  private String bodyAsString(Object body) {
    if (body instanceof byte[] bytes) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    return Objects.toString(body, "");
  }
}
