package dev.aulait.mousse.util;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
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
   * @param response the wrapper populated with the response returned by the remaining chain
   * @param <T> the response body type
   * @return the response returned by the next filter or the HTTP client
   */
  @Override
  public <T> HttpResponse<T> next(RequestWrapper request, ResponseWrapper<T> response) {
    HttpResponse<T> httpResponse;
    if (index < filters.size()) {
      httpResponse = filters.get(index++).filter(request, response, this);
    } else {
      httpResponse = sendRequest(request, response);
    }
    response.setResponse(httpResponse);
    return httpResponse;
  }

  private <T> HttpResponse<T> sendRequest(RequestWrapper request, ResponseWrapper<T> response) {
    try {
      return httpClientSupplier.get().send(request.getRequest(), response.getBodyHandler());
    } catch (IOException e) {
      throw new RestClientException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RestClientException(e);
    }
  }
}
