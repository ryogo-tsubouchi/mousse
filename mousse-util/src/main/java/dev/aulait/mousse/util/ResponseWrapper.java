package dev.aulait.mousse.util;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
public class ResponseWrapper<T> {
  private final ResponseType<T> responseType;

  @Getter(AccessLevel.PACKAGE)
  private final HttpResponse.BodyHandler<T> bodyHandler;

  private HttpResponse<T> response;
  private String plainBody;
  private T parsedBody;

  ResponseWrapper(ResponseType<T> responseType) {
    this.responseType = responseType;
    this.bodyHandler = bodyHandler(responseType.getType());
  }

  @SuppressWarnings("unchecked")
  private HttpResponse.BodyHandler<T> bodyHandler(Class<T> responseType) {
    if (responseType == byte[].class) {
      return (HttpResponse.BodyHandler<T>) HttpResponse.BodyHandlers.ofByteArray();
    } else {
      return (HttpResponse.BodyHandler<T>)
          HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    }
  }

  @Data
  public static class ResponseType<T> {
    private Class<T> type;
    private JsonType<T> jsonType;

    ResponseType(Class<T> type) {
      this.type = type;
    }

    ResponseType(JsonType<T> jsonType) {
      this.jsonType = jsonType;
    }
  }
}
