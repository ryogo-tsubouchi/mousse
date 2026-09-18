package dev.aulait.mousse.util;

import java.net.http.HttpResponse;

/** Continues processing with the next filter, or sends the request at the end of the chain. */
public interface FilterContext {

  /**
   * Continues processing the request.
   *
   * @param request the request to pass to the next filter
   * @param response the wrapper populated with the response returned by the remaining chain
   * @param <T> the response body type
   * @return the response returned by the remaining chain
   */
  <T> HttpResponse<T> next(RequestWrapper request, ResponseWrapper<T> response);
}
