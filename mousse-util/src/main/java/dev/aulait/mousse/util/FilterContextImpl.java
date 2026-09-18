package dev.aulait.mousse.util;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.List;
import java.util.function.Supplier;

class FilterContextImpl implements FilterContext {

  private final List<RestClientFilter> filters;
  private final Supplier<HttpClient> httpClientSupplier;
  private int index;

  FilterContextImpl(List<RestClientFilter> filters, Supplier<HttpClient> httpClientSupplier) {
    this.filters = filters;
    this.httpClientSupplier = httpClientSupplier;
  }

  /**
   * Invokes the next filter, or sends the HTTP request when no filters remain.
   *
   * <p>Each filter must call {@code context.next(request, response)} to continue the chain. Without
   * this call, the remaining filters are not invoked and the HTTP request is not sent.
   *
   * @param request the request to pass to the next filter or send
   * @param response the wrapper populated with the HTTP response by the remaining chain
   * @param <T> the response body type
   */
  @Override
  public <T> void next(RequestWrapper request, ResponseWrapper<T> response) {
    if (index < filters.size()) {
      filters.get(index++).filter(request, response, this);
    } else {
      sendRequest(request, response);
    }
  }

  private <T> void sendRequest(RequestWrapper request, ResponseWrapper<T> response) {
    try {
      response.setResponse(
          httpClientSupplier.get().send(request.getRequest(), response.getBodyHandler()));
    } catch (IOException e) {
      throw new RestClientException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RestClientException(e);
    }
  }
}
