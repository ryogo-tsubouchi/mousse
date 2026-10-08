package dev.aulait.mousse.util.restclient;

import java.net.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

/** Logs response details around a {@link RestClient} HTTP call. */
@Slf4j
public class ResponseLoggingFilter implements RestClientFilter {

  private final HeaderLogMasker headerLogMasker = new HeaderLogMasker();
  private final LoggingItems loggingItems = new LoggingItems();

  @Override
  public <T> void filter(
      RequestWrapper request, ResponseWrapper<T> responseWrapper, FilterContext context) {
    context.next(request, responseWrapper);
    HttpResponse<T> response = responseWrapper.getResponse();
    if (loggingItems.includes(LoggingItems.Item.STATUS)) {
      log.info("Response status: {}", response.statusCode());
    }
    if (loggingItems.includes(LoggingItems.Item.HEADER)) {
      log.info("Response headers: {}", headerLogMasker.mask(response.headers().map()));
    }
    if (loggingItems.includes(LoggingItems.Item.BODY) && log.isDebugEnabled()) {
      log.debug("Response body: {}", responseWrapper.bodyAsString());
    }
  }
}
