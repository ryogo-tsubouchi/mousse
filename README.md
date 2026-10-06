# Mousse

Mousse is a lightweight Java utility library that provides the following features:

- **BeanUtils**: Bean mapping between different types using ModelMapper
- **JsonUtils**: JSON serialization and deserialization using Jackson
- **RestClient**: HTTP REST client using Java's built-in `HttpClient`

## Required Software

- Java 21+
- Maven

## Usage

To use Mousse, add the following dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>dev.aulait.mousse</groupId>
  <artifactId>mousse-util</artifactId>
  <version>0.8</version>
</dependency>
```

### BeanUtils

`BeanUtils` provides static methods to map objects between different types using [ModelMapper](https://modelmapper.org/).

#### Basic mapping

```java
import dev.aulait.mousse.util.BeanUtils;

UserEntity entity = userRepository.findById(id);

// Map to a different type
UserDto dto = BeanUtils.map(entity, UserDto.class);
```

#### Mapping generic types

Use `BeanType` when mapping to generic types such as `List<T>`:

```java
import dev.aulait.mousse.util.BeanUtils;
import dev.aulait.mousse.util.BeanType;

Set<UserEntity> entities = userRepository.findAll();

List<UserDto> dtos = BeanUtils.map(entities, new BeanType<List<UserDto>>() {});
```

#### Custom type maps

Register a named type map to customize mapping behavior:

```java
import dev.aulait.mousse.util.BeanUtils;

BeanUtils.registerTypeMap(UserEntity.class, UserDto.class, "UserEntityToUserDto")
    .addMappings(mapper -> mapper.skip(UserDto::setPassword))
    .implicitMappings();

UserDto dto = BeanUtils.map(entity, UserDto.class, "UserEntityToUserDto");
// dto.getPassword() == null
```

---

### JsonUtils

`JsonUtils` provides static methods for JSON serialization and deserialization using [Jackson](https://github.com/FasterXML/jackson).

#### Convert object to JSON string

```java
import dev.aulait.mousse.util.JsonUtils;

UserDto dto = new UserDto("alice", "alice@example.com");

String json = JsonUtils.obj2str(dto);
// {"name":"alice","email":"alice@example.com"}
```

#### Convert JSON string to object

```java
import dev.aulait.mousse.util.JsonUtils;
import dev.aulait.mousse.util.JsonType;

String json = "[{\"name\":\"alice\"},{\"name\":\"bob\"}]";

List<UserDto> users = JsonUtils.str2obj(json, new JsonType<List<UserDto>>() {});
```

#### Read from / write to file

```java
import dev.aulait.mousse.util.JsonUtils;
import java.nio.file.Path;

// Write
JsonUtils.obj2file(dto, Path.of("user.json"));

// Read
UserDto dto = JsonUtils.file2obj(Path.of("user.json"), UserDto.class);
```

---

### RestClient

`RestClient` is an HTTP client that supports GET, POST, PUT, and DELETE operations with automatic JSON serialization/deserialization.

#### Setup

```java
import dev.aulait.mousse.util.restclient.RestClient;

RestClient client = new RestClient("https://api.example.com");

// Optional: set an access token for Bearer authentication
client.setAccessToken(accessToken);
```

#### GET

```java
// Single object
UserDto user = client.get("/users/{id}", UserDto.class, userId);

// List
List<UserDto> users = client.getAsList("/users", new JsonType<List<UserDto>>() {});

// Binary (e.g. file download)
byte[] bytes = client.getAsByte("/files/{id}", fileId);
```

#### POST

```java
UserDto created = client.post("/users", newUser, UserDto.class);
```

#### Multipart POST

```java
import java.nio.file.Path;
import java.util.Map;

Map<String, Object> parts = new LinkedHashMap<>();
parts.put("file", Path.of("document.pdf"));
parts.put("description", "My document");

UploadResult result = client.postMultipart("/upload", parts, UploadResult.class);
```

#### PUT

```java
UserDto updated = client.put("/users/{id}", updatedUser, UserDto.class, userId);
```

#### DELETE

```java
client.delete("/users/{id}", null, Void.class, userId);
```

#### Error handling

`RestClient` throws `RestClientException` when the response status is not 2xx:

```java
import dev.aulait.mousse.util.restclient.RestClientException;

try {
    UserDto user = client.get("/users/{id}", UserDto.class, userId);
} catch (RestClientException e) {
    int status = e.getStatusCode();
    String body = e.getBody();
}
```

#### Header logging

Use a builder preconfigured with the default masked headers, then add custom masks and configure
header selection and exclusions before passing the settings to the logging filters:

```java
import dev.aulait.mousse.util.restclient.HeaderLogConfig;
import dev.aulait.mousse.util.restclient.RequestLoggingFilter;
import dev.aulait.mousse.util.restclient.ResponseLoggingFilter;
import dev.aulait.mousse.util.restclient.RestClient;
import java.util.List;
import java.util.Set;

HeaderLogConfig headers =
  HeaderLogConfig.builderWithDefaultMaskedHeaders()
    .maskedHeaders(Set.of("X-Api-Key"))
    .includedHeaders(Set.of("Content-Type", "Authorization", "X-Api-Key", "X-Request-Id", "X-Internal"))
    .excludedHeaders(Set.of("X-Internal"))
    .build();

RestClient client = RestClient.builder()
        .baseUrl("https://api.example.com")
        .filters(List.of(new RequestLoggingFilter(headers), new ResponseLoggingFilter(headers)))
        .build();
```

    In this example, only the included headers can appear in request and response header logs.
    `X-Internal` is omitted even though it is included. `Authorization` and `X-Api-Key` are masked,
    while `Content-Type` and `X-Request-Id` retain their values.

- Names are case-insensitive. Rules select included headers, remove excluded headers, then mask
  the remaining sensitive values. Exclusion takes precedence over inclusion and masking.
- Omitting `includedHeaders` (or passing `null`) selects all headers. `Set.of()` selects none.
- Omitting `excludedHeaders` or passing `null` excludes none.
- `builderWithDefaultMaskedHeaders()` returns a builder with `Authorization`, `Proxy-Authorization`,
  `Cookie`, and `Set-Cookie` configured for masking. Selection and exclusion can still be customized.
- `builder()` has no default masks. `maskedHeaders` adds to the configured headers without replacing
  existing masks, including the defaults. Repeated calls accumulate headers; an empty or null set
  leaves the current configuration unchanged. Every masked value, including each value of a
  multi-valued header, becomes `***`.
- No-argument logging filters do not mask headers. Pass an explicit configuration as above to
  avoid logging sensitive header values in plain text.
- Settings are immutable and can be shared, or different settings can be passed to each filter.
  Header names retain their original spelling; logging never modifies request or response values.
- Method, URI, status, and body logging are unchanged. Bodies at DEBUG and URIs are **not masked**;
  avoid putting secrets there when using these filters.
