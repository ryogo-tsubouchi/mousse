package dev.aulait.mousse.util;

import java.net.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

/** Logs request details around a {@link RestClient} HTTP call. */
@Slf4j
public class RequestLoggingFilter implements RestClientFilter {

  @Override
  public <T> HttpResponse<T> filter(
      RestClientRequest request, HttpResponse.BodyHandler<T> bodyHandler, FilterContext context) {
    log.info("Request method: {}", request.method());
    log.info("Request URI: {}", request.uri());
    log.info("Request headers: {}", HeaderLogFormatter.format(request.headers()));
    if (log.isDebugEnabled()) {
      log.debug("Request body: {}", request.bodyAsString());
    }
    return context.next(request, bodyHandler);
  }
}
