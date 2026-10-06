package dev.aulait.mousse.util.restclient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;

/** Immutable header selection and masking rules used only for logging. */
public final class HeaderLogConfig {

  private static final Set<String> DEFAULT_MASKED_HEADERS =
      Set.of("authorization", "proxy-authorization", "cookie", "set-cookie");

  private final Set<String> includedHeaders;
  private final Set<String> excludedHeaders;
  private final Set<String> maskedHeaders;

  /**
   * Creates logging rules. Header names are case-insensitive.
   *
   * @param includedHeaders null selects all headers; an empty set selects none
   * @param excludedHeaders headers to omit after selection; null excludes none
   * @param maskedHeaders headers to mask; null masks none. Every value is replaced with {@code
   *     ***}.
   */
  @Builder
  private HeaderLogConfig(
      Set<String> includedHeaders, Set<String> excludedHeaders, Set<String> maskedHeaders) {
    this.includedHeaders = includedHeaders == null ? null : normalize(includedHeaders);
    this.excludedHeaders = excludedHeaders == null ? Set.of() : normalize(excludedHeaders);
    this.maskedHeaders = maskedHeaders == null ? Set.of() : normalize(maskedHeaders);
  }

  public static HeaderLogConfigBuilder builderWithDefaultMaskedHeaders() {
    return HeaderLogConfig.builder().maskedHeaders(DEFAULT_MASKED_HEADERS);
  }

  public static class HeaderLogConfigBuilder {
    private Set<String> maskedHeaders;

    public HeaderLogConfigBuilder maskedHeaders(Set<String> additionalHeaders) {
      if (additionalHeaders == null) {
        return this;
      }

      maskedHeaders =
          maskedHeaders == null
              ? additionalHeaders
              : Stream.concat(maskedHeaders.stream(), additionalHeaders.stream())
                  .collect(Collectors.toUnmodifiableSet());
      return this;
    }
  }

  private static Set<String> normalize(Set<String> names) {
    return names.stream()
        .map(name -> name.toLowerCase(Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());
  }

  Map<String, List<String>> headersForLogging(Map<String, List<String>> headers) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    headers.forEach(
        (name, values) -> {
          String normalizedName = name.toLowerCase(Locale.ROOT);
          if ((includedHeaders == null || includedHeaders.contains(normalizedName))
              && !excludedHeaders.contains(normalizedName)) {
            result.put(
                name,
                maskedHeaders.contains(normalizedName)
                    ? values.stream().map(value -> "***").toList()
                    : List.copyOf(values));
          }
        });
    return result;
  }
}
