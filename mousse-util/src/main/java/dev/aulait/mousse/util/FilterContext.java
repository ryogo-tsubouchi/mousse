package dev.aulait.mousse.util;

/** Continues processing with the next filter, or sends the request at the end of the chain. */
public interface FilterContext {

  /**
   * Continues processing the request.
   *
   * @param request the request to pass to the next filter
   * @param response the wrapper populated with the HTTP response by the remaining chain
   * @param <T> the response body type
   */
  <T> void next(RequestWrapper request, ResponseWrapper<T> response);
}
