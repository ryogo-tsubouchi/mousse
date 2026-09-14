package dev.aulait.mousse.util;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class HeaderLogFormatter {

  private static final Set<String> LOGGABLE_HEADERS =
      Set.of(
          "accept",
          "accept-encoding",
          "accept-language",
          "cache-control",
          "content-length",
          "content-type",
          "user-agent",
          "x-request-id");

  private HeaderLogFormatter() {}

  static Map<String, List<String>> format(HttpHeaders headers) {
    Map<String, List<String>> formatted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headers.map().entrySet().stream()
        .filter(entry -> LOGGABLE_HEADERS.contains(entry.getKey().toLowerCase(Locale.ROOT)))
        .forEach(entry -> formatted.put(entry.getKey(), entry.getValue()));
    return formatted;
  }
}
