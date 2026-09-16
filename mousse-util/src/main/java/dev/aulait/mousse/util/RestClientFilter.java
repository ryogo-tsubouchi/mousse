package dev.aulait.mousse.util;

import java.net.http.HttpResponse;

/** Intercepts a REST request and its response around the actual HTTP call. */
@FunctionalInterface
public interface RestClientFilter {

  /**
   * Filters a request and response.
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
