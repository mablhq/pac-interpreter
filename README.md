<img src="https://www.mabl.com/favicon.ico" width="128" />

# pac-interpreter

A [Proxy Auto-Configuration](https://developer.mozilla.org/en-US/docs/Web/HTTP/Proxy_servers_and_tunneling/Proxy_Auto-Configuration_PAC_file)
(PAC) script interpreter for Java with minimal dependencies.

## Installation

### Gradle

```
implementation group: 'com.mabl', name: 'pac-interpreter', version: '1.+'
```

### Maven

```
<dependency>
    <groupId>com.mabl</groupId>
    <artifactId>pac-interpreter</artifactId>
    <version>[1.0,2.0)</version>
</dependency>
```

## Quickstart

Please see the full documentation below this example for more information.

```
import com.mabl.net.proxy.FindProxyDirective;
import com.mabl.net.proxy.FindProxyResult;
import com.mabl.net.proxy.PacInterpreter;
import com.mabl.net.proxy.ReloadablePacInterpreter;
import java.net.URL;
import java.time.Duration;
...

// Initialize the interpreter:
PacInterpreter interpreter = ReloadablePacInterpreter.forUrl(new URL("https://example.com/proxy.pac"));

// Reload the PAC from https://example.com/proxy.pac every 5 minutes
interpreter.start(Duration.ofMinutes(5));

// Find the proxy that should be used for the URL https://www.example.org/foo :
FindProxyResult result = interpreter.findProxyForUrl("https://www.example.com/foo");
FindProxyDirective firstDirective = result.first();

// Connect as described by the directive:
switch (firstDirective.connectionType()) {
  case DIRECT:
    System.out.println("Connecting without proxy");
    break;
  case HTTP:
  case HTTPS:
  case PROXY:
    System.out.println("Connecting via HTTP(S) proxy " + firstDirective.proxyHostAndPort());
    break;
  case SOCKS:
  case SOCKS4:
  case SOCKS5:
    System.out.println("Connecting via SOCKS proxy " + firstDirective.proxyHostAndPort());
    break;
}

...

interpreter.stop(); // Stop automatic PAC reloads
```

## Usage

This package provides two PAC interpreter implemenations, both of which implement
the [PacInterpreter](/blob/main/src/main/java/com/mabl/net/proxy/PacInterpreter.java) interface:

1. [SimplePacInterpreter](/blob/main/src/main/java/com/mabl/net/proxy/SimplePacInterpreter.java)
1. [AutoReloadingPacInterpreter](/blob/main/src/main/java/com/mabl/net/proxy/AutoReloadingPacInterpreter.java)

### `SimplePacInterpreter`

The [SimplePacInterpreter](/blob/main/src/main/java/com/mabl/net/proxy/SimplePacInterpreter.java) implementation loads a
specified PAC script once and provides a method to execute the PAC's `FindProxyForURL` function as many times as needed.

The PAC script can be loaded into the interpreter in several ways:

#### Passed directly as a `String`

```
import com.mabl.net.proxy.PacInterpreter;
import com.mabl.net.proxy.SimplePacInterpreter;
...
String script = "function FindProxyForURL(url, host) { return \"DIRECT\"; }";
PacInterpreter interpreter = SimplePacInterpreter.forScript(script);
```

#### Loaded from a `File`

```
import com.mabl.net.proxy.PacInterpreter;
import com.mabl.net.proxy.SimplePacInterpreter;
import java.io.File;
...
PacInterpreter interpreter = SimplePacInterpreter.forFile(new File("/path/to/proxy.pac"));
```

#### Loaded from a URL

```
import com.mabl.net.proxy.PacInterpreter;
import com.mabl.net.proxy.SimplePacInterpreter;
import java.net.URL;
...
PacInterpreter interpreter = SimplePacInterpreter.forUrl(new URL("https://example.com/proxy.pac"));
```

### `ReloadablePacInterpreter`

The [ReloadablePacInterpreter](/blob/main/src/main/java/com/mabl/net/proxy/ReloadablePacInterpreter.java) implementation
is similar to `SimplePacInterpreter` except that it allows the PAC to be reloaded, either explicitly by executing
the `reload()` method or automatically in the background at a specified period using the `start(Duration)` method. To
stop automatic reloading, execute the `stop()` method.

The `ReloadablePacInterpreter` is initialized in a similar way to `SimplePacInterpreter`, with various options for
loading the PAC script:

#### Passed directly as a `String`

Passing the script directly to `ReloadablePacInterpreter` is slightly different than
with `SimplePacInterpreter`.  `ReloadablePacInterpreter` takes a `Supplier<String>` rather than a `String` in order to
allow the underlying script to change between reloads:

```
import com.mabl.net.proxy.PacInterpreter;
import com.mabl.net.proxy.ReloadablePacInterpreter;
...
private String loadScript() {
  // TODO Get the script from somewhere
  return "function FindProxyForURL(url, host) { return \"DIRECT\"; }";
}
PacInterpreter interpreter = ReloadablePacInterpreter.forScript(this::loadScript);
```

#### Loaded from a `File`

Each time the interpreter is reloaded, the file will be re-read.

```
import com.mabl.net.proxy.PacInterpreter;
import com.mabl.net.proxy.ReloadablePacInterpreter;
import java.io.File;
...
PacInterpreter interpreter = ReloadablePacInterpreter.forFile(new File("/path/to/proxy.pac"));
```

#### Loaded from a URL

Each time the interpreter is reloaded the PAC script will be re-fetched from the specified URL.

```
import com.mabl.net.proxy.PacInterpreter;
import com.mabl.net.proxy.ReloadablePacInterpreter;
import java.net.URL;
...
PacInterpreter interpreter = ReloadablePacInterpreter.forUrl(new URL("https://example.com/proxy.pac"));
```

#### Triggering a reload manually

To reload the PAC from the underlying source manually, call the `reload()` method:

```
import com.mabl.net.proxy.PacInterpreter;
import com.mabl.net.proxy.ReloadablePacInterpreter;
import java.net.URL;
...
PacInterpreter interpreter = ReloadablePacInterpreter.forUrl(new URL("https://example.com/proxy.pac"));
interpreter.reload();
```

#### Automatic reloads

To start automatic reloads, use the `start(Duration)` method. To stop, call `stop()`:

```
import com.mabl.net.proxy.PacInterpreter;
import com.mabl.net.proxy.ReloadablePacInterpreter;
import java.net.URL;
import java.time.Duration;
...
PacInterpreter interpreter = ReloadablePacInterpreter.forUrl(new URL("https://example.com/proxy.pac"));
interpreter.start(Duration.ofMinutes(5));
...
interpreter.stop();
```

### Using the interpreter to select a proxy

Once you have chosen an interpreter implementation and successfully initialized it, you can use that interpreter to
invoke the PAC script's `FindProxyForURL` function.

The easiest way to do this is to call the interpreter's `findProxyForUrl(String url)` method:

```
import com.mabl.net.proxy.PacInterpreter;
import com.mabl.net.proxy.FindProxyResult;
...
PacInterpreter interpreter = initializeInterpreter();
FindProxyResult result = interpreter.findProxyForUrl("https://www.example.com");
```

The PAC script's `FindProxyForURL` actually takes two arguments, the full URL and the
host ([defined](https://developer.mozilla.org/en-US/docs/Web/HTTP/Proxy_servers_and_tunneling/Proxy_Auto-Configuration_PAC_file#parameters)
as "the string between `://` and the first `:` or `/` after that"). The `PacInterpreter` will automatically parse the
host from the URL for you, but if you prefer to pass a custom value for `host`, you can call the overloaded version of
this method that takes two parameters:

```
import com.mabl.net.proxy.PacInterpreter;
import com.mabl.net.proxy.FindProxyResult;
...
PacInterpreter interpreter = initializeInterpreter();
FindProxyResult result = interpreter.findProxyForUrl("https://www.example.com", "www.example.com");
```

### The `FindProxyResult`

The PAC script's `FindProxyForURL` function returns a string which might contain multiple proxy directives separated
by `;`. For example:

```
"PROXY 4.5.6.7:8080; PROXY 7.8.9.10:8080; DIRECT"
```

To make this output easier to use, the interpreter automatically parses it and returns it as an instance
of `FindProxyResult`. The `FindProxyResult` class includes several methods for exploring these directives:

#### `size()`

Returns the number of proxy directives.

#### `all()`

Returns a `List<ProxyDirective>` containing all directives that were parsed from the result in the order in which
the `FindProxyForURL` function returned them.

#### `first()`

Returns the first `ProxyDirective`.

#### `random()`

Returns a random `ProxyDirective`.

#### `get(int index)`

Returns the `ProxyDirective` at the specified (zero-based) index.

#### Iterating

`FindProxyResult` is also an `Iterable<FindProxyDirective>`:

```
for (FindProxyDirective directive : findProxyResult) {
  ...
}
```

### The `FindProxyDirective`

`FindProxyDirective` allows you to obtain the connection type and the `host:port` of the proxy (if any).

The connection type is returned by the `connectionType()` method and is represented as an `enum` with one of the
following values:

* `DIRECT`
* `HTTP`
* `HTTPS`
* `PROXY`
* `SOCKS`
* `SOCKS4`
* `SOCKS5`

The hostname and port of the proxy (if any) can be obtained by calling the `proxyHostAndPort()` method. The host and
port will be returned as a `String` with the form `host:port`.

_Note: `proxyHostAndPort()` will return `null` if the connection type is `DIRECT`._

### GraalVM optimization

This PAC interpreter uses [GraalVM](https://www.graalvm.org/latest/reference-manual/polyglot-programming/) to execute
the JavaScript-based PAC scripts. To maximize GraalVM performance it is necessary to add certain arguments when starting
the JVM. See GraalVM's documentation
on [Running GraalVM JavaScript on a Stock JDK](https://www.graalvm.org/latest/reference-manual/js/RunOnJDK/) for more
information.

## Building

Use the `gradlew` script to build locally:

```
./gradlew build
```

## Contributing

Please feel free to file [issues](/issues) and submit [pull requests](/pulls) if you would like to contribute to this
project.

## License

This code is released under the [GNU Lesser General Public License v2.1](/blob/main/LICENSE), mainly because
the [PAC utility functions](/blob/main/src/main/resources/pacUtils.js) are derived from the original Mozilla
implementation which was released under that license.
