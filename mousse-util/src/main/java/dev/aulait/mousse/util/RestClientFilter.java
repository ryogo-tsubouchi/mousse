package dev.aulait.mousse.util;

/** Intercepts a REST request and its response around the actual HTTP call. */
@FunctionalInterface
public interface RestClientFilter {

  /**
   * Implementations must call {@link FilterContext#next(RequestWrapper, ResponseWrapper)
   * context.next(request, response)} to continue the filter chain and send the HTTP request.
   * Without this call, the remaining filters are not invoked and the HTTP request is not sent.
   * Setting the wrapper's HTTP response without calling {@code context.next} short-circuits the
   * chain.
   *
   * @param request the request to inspect or replace
   * @param response the response wrapper; its HTTP response is initially null and populated by
   *     {@code context.next}; body conversion happens after the filter chain completes
   * @param context the remaining filter chain
   * @param <T> the response body type
   */
  <T> void filter(RequestWrapper request, ResponseWrapper<T> response, FilterContext context);
}
