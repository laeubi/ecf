# HTTP/2 GOAWAY Test Project - Quick Start Guide

## What This Project Does

This standalone Maven project was created to test HTTP/2 GOAWAY frame handling as requested in issue about p2 download issues.

## Quick Start

```bash
cd tests/http2-goaway-test
mvn test
```

You should see output like:
```
✓ Normal request completed successfully
✓ First request completed, server will send GOAWAY after this
✓ Second request succeeded by opening new connection (GOAWAY handled correctly)
✓ Request 1 completed
✓ Request 2 completed, server will send GOAWAY after this
✓ Request 3 succeeded by opening new connection after GOAWAY
✓ Request succeeded despite immediate GOAWAY (new connection opened)

Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

## What We Learned

**Important:** Java 11's HttpClient handles HTTP/2 GOAWAY correctly! When a server sends GOAWAY:
- The client automatically opens a new connection
- Subsequent requests succeed seamlessly
- This is the CORRECT and expected behavior per HTTP/2 spec

## Project Structure

```
http2-goaway-test/
├── pom.xml                    # Maven configuration with Netty and JUnit 5
├── README.md                  # Full documentation
└── src/
    ├── main/java/             # HTTP/2 server implementation
    │   └── org/eclipse/ecf/tests/http2/goaway/
    │       ├── Http2ServerWithGoaway.java      # Main server
    │       ├── Http2ServerInitializer.java     # Pipeline setup
    │       └── Http2ServerHandler.java         # Request/GOAWAY handler
    └── test/java/             # Integration tests
        └── org/eclipse/ecf/tests/http2/goaway/
            └── Http2GoawayTest.java            # 4 test scenarios
```

## Test Scenarios

1. **Normal Request** - Baseline without GOAWAY
2. **GOAWAY After First Request** - Server sends GOAWAY after handling one request
3. **Multiple Requests With GOAWAY** - GOAWAY sent after N requests
4. **Immediate GOAWAY** - Server sends GOAWAY right on connection

All tests PASS, demonstrating Java HttpClient's resilience.

## Technologies Used

- **Java 11+**: For HttpClient and modern Java features
- **Netty 4.1.115**: For low-level HTTP/2 protocol control
- **JUnit 5**: For testing framework
- **BouncyCastle**: For self-signed certificate generation
- **Maven**: For build and dependency management

## Next Steps

This test infrastructure can be used to:
- Test edge cases and timing issues
- Develop retry strategies
- Test connection pool behavior
- Verify fixes for specific GOAWAY scenarios

## Related

- Issue: https://github.com/eclipse-equinox/p2/issues/529
- RFC 7540 Section 6.8: GOAWAY frame specification
