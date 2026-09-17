package dev.aulait.mousse.util;

import java.net.http.HttpResponse;

/** Intercepts a REST request and its response around the actual HTTP call. */
@FunctionalInterface
public interface RestClientFilter {

  /**
   * Implementations must call {@link FilterContext#next(RequestWrapper, HttpResponse.BodyHandler)
   * context.next(request, bodyHandler)} to continue the filter chain and send the HTTP request.
   * Without this call, the remaining filters are not invoked and the HTTP request is not sent.
   * Returning a response directly without calling {@code context.next} short-circuits the chain.
   *
   * @param request the request to inspect or replace
   * @param bodyHandler the response body handler
   * @param context the remaining filter chain
   * @param <T> the response body type
   * @return the response returned by the remaining chain
   */
  <T> HttpResponse<T> filter(
      RequestWrapper request, HttpResponse.BodyHandler<T> bodyHandler, FilterContext context);
}
