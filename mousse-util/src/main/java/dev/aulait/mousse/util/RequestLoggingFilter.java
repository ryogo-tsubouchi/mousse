package dev.aulait.mousse.util;

import lombok.extern.slf4j.Slf4j;

/** Logs request details around a {@link RestClient} HTTP call. */
@Slf4j
public class RequestLoggingFilter implements RestClientFilter {

  @Override
  public <T> void filter(
      RequestWrapper request, ResponseWrapper<T> response, FilterContext context) {
    log.info("Request method: {}", request.getRequest().method());
    log.info("Request URI: {}", request.getRequest().uri());
    log.info("Request headers: {}", request.getRequest().headers().map());
    if (log.isDebugEnabled()) {
      log.debug("Request body: {}", request.bodyAsString());
    }
    context.next(request, response);
  }
}
