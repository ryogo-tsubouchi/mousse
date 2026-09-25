package dev.aulait.mousse.util;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
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

  ResponseWrapper(Class<T> responseType) {
    this(new ResponseType<>(responseType));
  }

  ResponseWrapper(JsonType<T> responseType) {
    this(new ResponseType<>(responseType));
  }

  private ResponseWrapper(ResponseType<T> responseType) {
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

  public String bodyAsString() {
    if (plainBody != null) {
      return plainBody;
    }

    Object body = response.body();
    plainBody =
        body instanceof byte[] bytes
            ? new String(bytes, StandardCharsets.UTF_8)
            : Objects.toString(body, "");
    return plainBody;
  }

  @SuppressWarnings("unchecked")
  void convertBody() {
    if (parsedBody != null) {
      return;
    }

    if (responseType.getType() == HttpResponse.class) {
      parsedBody = (T) response;
      return;
    }

    parsedBody = responseType.convertBody(bodyAsString());
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

    @SuppressWarnings("unchecked")
    private T convertBody(String body) {
      if (type == Void.class || type == void.class) {
        return null;
      } else if (type == String.class) {
        return (T) body;
      } else if (type == Integer.class) {
        return (T) Integer.valueOf(body.trim());
      } else if (type == Long.class) {
        return (T) Long.valueOf(body.trim());
      } else if (type == UUID.class) {
        String value = body.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
          value = value.substring(1, value.length() - 1);
        }
        return (T) UUID.fromString(value);
      } else if (jsonType != null) {
        return JsonUtils.str2obj(body, jsonType);
      } else {
        return JsonUtils.str2obj(body, type);
      }
    }
  }
}
