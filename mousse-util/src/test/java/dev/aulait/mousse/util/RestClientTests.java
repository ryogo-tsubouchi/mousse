package dev.aulait.mousse.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class RestClientTests {

  static HttpServer server;
  static RestClient client;

  @BeforeAll
  static void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);

    server.createContext(
        "/api/items",
        exchange -> {
          switch (exchange.getRequestMethod()) {
            case "GET":
              String path = exchange.getRequestURI().getPath();
              String id = path.substring(path.lastIndexOf('/') + 1);
              sendResponse(exchange, 200, "{\"id\":\"" + id + "\",\"name\":\"Item " + id + "\"}");
              break;
            case "POST", "PUT":
              String body =
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
              sendResponse(exchange, 200, body);
              break;
            case "DELETE":
              exchange.getRequestBody().readAllBytes();
              sendResponse(exchange, 200, "\"deleted\"");
              break;
            default:
              sendResponse(exchange, 405, "");
          }
        });

    server.createContext(
        "/api/count",
        exchange -> {
          sendResponse(exchange, 200, "42");
        });

    server.createContext(
        "/api/uuid",
        exchange -> {
          sendResponse(exchange, 200, "\"550e8400-e29b-41d4-a716-446655440000\"");
        });

    server.createContext(
        "/api/long-value",
        exchange -> {
          sendResponse(exchange, 200, "9999999999");
        });

    server.createContext(
        "/api/items-list",
        exchange -> {
          sendResponse(
              exchange,
              200,
              "[{\"id\":\"1\",\"name\":\"Item 1\"},{\"id\":\"2\",\"name\":\"Item 2\"}]");
        });

    server.createContext(
        "/api/binary",
        exchange -> {
          byte[] bytes = new byte[] {0x01, 0x02, 0x03, 0x04};
          exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });

    server.createContext(
        "/api/auth-check",
        exchange -> {
          String auth = exchange.getRequestHeaders().getFirst("Authorization");
          if (auth != null) {
            sendResponse(exchange, 200, "{\"token\":\"" + auth + "\"}");
          } else {
            sendResponse(exchange, 200, "{\"token\":\"none\"}");
          }
        });

    server.createContext(
        "/api/multipart",
        exchange -> {
          String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
          boolean isMultipart =
              contentType != null && contentType.startsWith("multipart/form-data");
          exchange.getRequestBody().readAllBytes();
          sendResponse(exchange, 200, "{\"multipart\":" + isMultipart + "}");
        });

    server.createContext(
        "/api/not-found",
        exchange -> {
          sendResponse(exchange, 404, "{\"error\":\"not found\"}");
        });

    server.createContext(
        "/api/server-error",
        exchange -> {
          sendResponse(exchange, 500, "{\"error\":\"internal server error\"}");
        });

    server.start();
    int port = server.getAddress().getPort();
    client = RestClient.builder().baseUrl("http://localhost:" + port).build();
  }

  @AfterAll
  static void tearDown() {
    server.stop(0);
  }

  @Test
  void requestWithoutBodyTest() {
    java.net.http.HttpRequest request =
        java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost/items"))
            .GET()
            .build();

    RestClientRequest clientRequest = new RestClientRequest(request);

    assertEquals(request, clientRequest.request());
    assertEquals("", clientRequest.bodyAsString());
    assertThrows(NullPointerException.class, () -> new RestClientRequest(null));
  }

  @Test
  void requestPreservesBodyTest() {
    byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
    java.net.http.HttpRequest request =
        java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost/items"))
            .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
    RestClientRequest clientRequest = new RestClientRequest(request, body);

    body[0] = 'X';

    assertEquals("payload", clientRequest.bodyAsString());
  }

  @Test
  void getTest() {
    Item item = client.get("/api/items/{id}", Item.class, "1");
    assertEquals("1", item.getId());
    assertEquals("Item 1", item.getName());
  }

  @Test
  void getAsStringTest() {
    String result = client.get("/api/items/{id}", String.class, "1");
    assertTrue(result.contains("\"id\":\"1\""));
  }

  @Test
  void getAsIntegerTest() {
    Integer count = client.get("/api/count", Integer.class);
    assertEquals(42, count);
  }

  @Test
  void getAsUuidTest() {
    UUID uuid = client.get("/api/uuid", UUID.class);
    assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), uuid);
  }

  @Test
  void getAsLongTest() {
    Long value = client.get("/api/long-value", Long.class);
    assertEquals(9999999999L, value);
  }

  @Test
  void getAsListTest() {
    List<Item> items = client.get("/api/items-list", new JsonType<List<Item>>() {});
    assertEquals(2, items.size());
    assertEquals("1", items.get(0).getId());
    assertEquals("2", items.get(1).getId());
  }

  @Test
  void getAsByteTest() {
    byte[] bytes = client.getAsByte("/api/binary");
    assertEquals(4, bytes.length);
    assertEquals(0x01, bytes[0]);
    assertEquals(0x04, bytes[3]);
  }

  @Test
  void postMultipartTest() {
    java.util.Map<String, Object> parts = new java.util.LinkedHashMap<>();
    parts.put("field1", "value1");
    parts.put("file1", new byte[] {0x01, 0x02});
    String result = client.postMultipart("/api/multipart", parts, String.class);
    assertTrue(result.contains("\"multipart\":true"));
  }

  @Test
  void postTest() {
    Item item = Item.of("1", "New Item");
    Item result = client.post("/api/items", item, Item.class);
    assertEquals("1", result.getId());
    assertEquals("New Item", result.getName());
  }

  @Test
  void loggingFiltersLogRequestAndResponseTest() {
    Logger requestLogger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    Logger responseLogger = (Logger) LoggerFactory.getLogger(ResponseLoggingFilter.class);
    Level originalRequestLevel = requestLogger.getLevel();
    Level originalResponseLevel = responseLogger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    requestLogger.addAppender(appender);
    responseLogger.addAppender(appender);

    String baseUrl = "http://localhost:" + server.getAddress().getPort();
    RestClient loggingClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .header("X-Request-Id", "logging-test")
            .header("Authorization", "Bearer test-secret")
            .filters(List.of(new RequestLoggingFilter(), new ResponseLoggingFilter()))
            .build();

    try {
      requestLogger.setLevel(Level.DEBUG);
      responseLogger.setLevel(Level.DEBUG);
      loggingClient.post("/api/items", Item.of("1", "Logged Item"), Item.class);

      List<String> messages =
          appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
      assertEquals(7, messages.size());
      assertTrue(messages.get(2).startsWith("Request headers: "));
      assertTrue(messages.get(2).contains("X-Request-Id=[logging-test]"));
      assertTrue(!messages.get(2).contains("Authorization"));
      assertTrue(messages.get(5).startsWith("Response headers: "));
      assertTrue(messages.get(5).contains("content-type=[application/json; charset=UTF-8]"));
      assertEquals(Level.INFO, appender.list.get(2).getLevel());
      assertEquals(Level.INFO, appender.list.get(5).getLevel());
      assertEquals(
          List.of(
              "Request method: POST",
              "Request URI: " + baseUrl + "/api/items",
              "Request body: {\"id\":\"1\",\"name\":\"Logged Item\"}",
              "Response status: 200",
              "Response body: {\"id\":\"1\",\"name\":\"Logged Item\"}"),
          List.of(
              messages.get(0), messages.get(1), messages.get(3), messages.get(4), messages.get(6)));

      requestLogger.setLevel(Level.INFO);
      responseLogger.setLevel(Level.INFO);
      appender.list.clear();
      loggingClient.post("/api/items", Item.of("1", "Logged Item"), Item.class);

      List<String> infoMessages =
          appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
      assertEquals(5, infoMessages.size());
      assertEquals(messages.get(2), infoMessages.get(2));
      assertTrue(infoMessages.get(4).startsWith("Response headers: "));
      assertTrue(infoMessages.get(4).contains("content-type=[application/json; charset=UTF-8]"));
      assertTrue(appender.list.stream().allMatch(event -> event.getLevel() == Level.INFO));
    } finally {
      requestLogger.setLevel(originalRequestLevel);
      responseLogger.setLevel(originalResponseLevel);
      requestLogger.detachAppender(appender);
      responseLogger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void headerLoggingMasksSecretsAndPreservesMultipleValuesTest() {
    Map<String, List<String>> values =
        Map.of(
            "aUtHoRiZaTiOn", List.of("Bearer secret"),
            "Proxy-Authorization", List.of("Basic secret"),
            "Cookie", List.of("session=secret"),
            "Set-Cookie", List.of("session=secret", "token=secret"),
            "X-API-Key", List.of("secret"),
            "x-request-id", List.of("request-1", "request-2"),
            "X-Trace", List.of("first", "second"));
    HttpHeaders headers = HttpHeaders.of(values, (name, value) -> true);

    Map<String, List<String>> formatted = HeaderLogFormatter.format(headers);

    assertEquals(Map.of("x-request-id", List.of("request-1", "request-2")), formatted);
    assertEquals(values, headers.map());
    assertTrue(
        HeaderLogFormatter.format(HttpHeaders.of(Map.of(), (name, value) -> true)).isEmpty());
  }

  @Test
  void putTest() {
    Item item = Item.of("1", "Updated Item");
    Item result = client.put("/api/items/{id}", item, Item.class, "1");
    assertEquals("1", result.getId());
    assertEquals("Updated Item", result.getName());
  }

  @Test
  void deleteTest() {
    String result = client.delete("/api/items/{id}", null, String.class, "1");
    assertEquals("\"deleted\"", result);
  }

  @Test
  void resolvePathTest() {
    String resolved = client.resolvePath(null, "/api/{resource}/{id}", "items", "123");
    assertTrue(resolved.endsWith("/api/items/123"));

    resolved = client.resolvePath("host", "/api/path");
    assertEquals("host/api/path", resolved);

    resolved = client.resolvePath("host/", "/api/path");
    assertEquals("host/api/path", resolved);

    resolved = client.resolvePath("host", "api/path");
    assertEquals("host/api/path", resolved);
  }

  @Test
  void notFoundThrowsRestClientExceptionTest() {
    RestClientException ex =
        assertThrows(RestClientException.class, () -> client.get("/api/not-found", Item.class));
    assertEquals(404, ex.getStatusCode());
    assertTrue(ex.getBody().contains("not found"));
  }

  @Test
  void serverErrorThrowsRestClientExceptionTest() {
    RestClientException ex =
        assertThrows(RestClientException.class, () -> client.get("/api/server-error", Item.class));
    assertEquals(500, ex.getStatusCode());
    assertTrue(ex.getBody().contains("internal server error"));
  }

  private static void sendResponse(HttpExchange exchange, int statusCode, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  @AllArgsConstructor(staticName = "of")
  @Builder
  @Data
  @NoArgsConstructor
  static class Item {
    String id;
    String name;
  }
}
