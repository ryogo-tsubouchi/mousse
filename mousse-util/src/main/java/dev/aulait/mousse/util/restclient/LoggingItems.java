package dev.aulait.mousse.util.restclient;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

final class LoggingItems {

  enum Item {
    STATUS,
    HEADER,
    BODY,
    METHOD,
    URI
  }

  private final Set<Item> items = EnumSet.allOf(Item.class);

  LoggingItems() {
    ConfigProviderResolver.instance()
        .getConfig()
        .getOptionalValue("mousse.rest-client.logging.items", String.class)
        .ifPresent(
            value -> {
              Set<Item> configuredItems = EnumSet.noneOf(Item.class);
              Arrays.stream(value.split(","))
                  .map(String::trim)
                  .filter(item -> !item.isEmpty())
                  .map(item -> Item.valueOf(item.toUpperCase(Locale.ROOT)))
                  .forEach(configuredItems::add);
              if (!configuredItems.isEmpty()) {
                items.clear();
                items.addAll(configuredItems);
              }
            });
  }

  boolean includes(Item item) {
    return items.contains(item);
  }
}
