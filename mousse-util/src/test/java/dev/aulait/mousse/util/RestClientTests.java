package dev.aulait.mousse.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.event.Level;

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

    server.createContext(
        "/api/connection-closed",
        exchange -> {
          try (exchange) {
            exchange.getRequestBody().readAllBytes();
          }
        });

    server.createContext(
        "/api/filter-order",
        exchange -> {
          String body =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          TestLoggerFactory.getTestLogger("filter-order-http-exchange")
              .info("HTTP request received");
          sendResponse(exchange, 200, body);
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

  @Nested
  class LoggingFiltersTests {
    TestLogger requestLogger;
    TestLogger responseLogger;
    RestClient.RestClientBuilder loggingClientBuilder;

    @BeforeEach
    void setUpLogging() {
      TestLoggerFactory.clearAll();
      requestLogger = TestLoggerFactory.getTestLogger(RequestLoggingFilter.class);
      responseLogger = TestLoggerFactory.getTestLogger(ResponseLoggingFilter.class);
      requestLogger.setEnabledLevels(Level.INFO, Level.DEBUG);
      responseLogger.setEnabledLevels(Level.INFO, Level.DEBUG);
      loggingClientBuilder =
          RestClient.builder()
              .baseUrl(client.getBaseUrl())
              .filters(List.of(new RequestLoggingFilter(), new ResponseLoggingFilter()));
    }

    @Test
    @Timeout(10)
    void loggingFiltersLogRequestAndResponseTest() {
      TestLoggerFactory.getTestLogger("filter-order-http-exchange")
          .setEnabledLevelsForAllThreads(Level.INFO);
      RestClient loggingClient =
          loggingClientBuilder
              .header("X-Request-Id", "logging-test")
              .header("Authorization", "Bearer test-secret")
              .build();
      String path = "/api/filter-order";
      String expectedRequestHeaders =
          "Request headers: {Accept=[*/*], Accept-Language=["
              + Locale.getDefault().toString().replace("_", "-")
              + "], Authorization=[Bearer test-secret], Content-Type=[application/json;"
              + " charset=UTF-8], X-Request-Id=[logging-test]}";
      String expectedResponseHeaders =
          "Response headers: {content-length=[31], content-type=[application/json; charset=UTF-8],"
              + " date=[<date>]}";

      Item requestBody = Item.of("1", "Logged Item");

      Item response = loggingClient.post(path, requestBody, Item.class);

      assertEquals(requestBody, response);
      assertEquals(
          List.of(
              "Request method: POST",
              "Request URI: " + loggingClient.getBaseUrl() + path,
              expectedRequestHeaders,
              "Request body: {\"id\":\"1\",\"name\":\"Logged Item\"}",
              "HTTP request received",
              "Response status: 200",
              expectedResponseHeaders,
              "Response body: {\"id\":\"1\",\"name\":\"Logged Item\"}"),
          TestLoggerFactory.getAllLoggingEvents().stream()
              .map(LoggingEvent::getFormattedMessage)
              .map(message -> message.replaceFirst("date=\\[[^\\]]+\\]", "date=[<date>]"))
              .toList());
    }

    @ParameterizedTest
    @CsvSource({"/api/not-found, 404, not found", "/api/server-error, 500, internal server error"})
    void loggingFiltersLogErrorResponseTest(String path, int statusCode, String errorMessage) {
      RestClient loggingClient = loggingClientBuilder.build();
      String expectedBody = "{\"error\":\"" + errorMessage + "\"}";

      RestClientException exception =
          assertThrows(RestClientException.class, () -> loggingClient.get(path, Item.class));

      assertEquals(statusCode, exception.getStatusCode());
      assertEquals(expectedBody, exception.getBody());
      List<String> responseMessages =
          responseLogger.getLoggingEvents().stream()
              .map(LoggingEvent::getFormattedMessage)
              .toList();
      assertTrue(responseMessages.contains("Response status: " + statusCode));
      assertTrue(responseMessages.contains("Response body: " + expectedBody));
    }

    @Test
    @Timeout(10)
    void loggingFiltersLogRequestOnIOExceptionTest() {
      RestClient loggingClient = loggingClientBuilder.build();
      String path = "/api/connection-closed";
      Item requestBody = Item.of("1", "Logged Item");
      RestClientException exception =
          assertThrows(
              RestClientException.class, () -> loggingClient.post(path, requestBody, Item.class));

      assertInstanceOf(IOException.class, exception.getCause());
      assertFalse(requestLogger.getLoggingEvents().isEmpty());
      assertTrue(responseLogger.getLoggingEvents().isEmpty());
    }
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
