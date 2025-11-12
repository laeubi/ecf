# Using Cleartext HTTP/2 (h2c)

The server supports cleartext HTTP/2, but Java's HttpClient requires special setup.

## Server Setup

```java
// Create server in cleartext mode
Http2ServerWithGoaway server = new Http2ServerWithGoaway(8080, false);
server.start();
```

## Client Options

### Option 1: Use curl (simplest for testing)

```bash
# curl with --http2-prior-knowledge flag
curl --http2-prior-knowledge http://localhost:8080/test -v
```

### Option 2: Use a different Java client

Libraries like OkHttp or Apache HttpClient 5 have better h2c support than Java's built-in HttpClient.

### Option 3: Java HttpClient (complex)

Java's HttpClient doesn't easily support h2c without prior knowledge negotiation. It tries HTTP/1.1 first by default.

## Why Tests Use TLS

The existing tests use TLS (h2) because:

1. **Simpler**: Java's HttpClient works out-of-the-box with TLS
2. **Reliable**: ALPN negotiation is standard and well-supported
3. **Same behavior**: GOAWAY handling is identical for h2 and h2c
4. **Minimal overhead**: Self-signed certificates add minimal complexity

## When to Use Cleartext

Use cleartext mode when:
- Testing with curl or other tools that support h2c
- Integrating with clients that have good h2c support
- Avoiding certificate management in specific test scenarios

The GOAWAY frame behavior is identical in both modes - the choice is about transport convenience.
