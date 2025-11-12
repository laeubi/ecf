# HTTP/2 GOAWAY Test Project

## Overview

This standalone Maven project reproduces HTTP/2 GOAWAY handling scenarios as described in https://github.com/eclipse-equinox/p2/issues/529.

The project provides:
- A controllable HTTP/2 server built with Netty that can send GOAWAY frames on demand
- Integration tests using Java 11's HttpClient to demonstrate client behavior when GOAWAY is received
- Different test scenarios: immediate GOAWAY, GOAWAY after N requests, etc.

## Purpose

The purpose of this test project is to:
1. Reliably reproduce HTTP/2 GOAWAY scenarios in a controlled environment
2. Demonstrate how Java's HttpClient handles GOAWAY frames
3. Provide a foundation for testing potential fixes and workarounds
4. Be independent from the OSGi bundles to allow easy experimentation

## Requirements

- Java 11 or higher (for HttpClient and modern Java features)
- Maven 3.6 or higher

## Building

```bash
mvn clean install
```

## Running Tests

```bash
mvn test
```

## Key Findings

**Important Discovery:** Java 11's HttpClient handles HTTP/2 GOAWAY frames **correctly and gracefully** by automatically opening new connections when the old connection is closed. This is the expected and proper behavior according to HTTP/2 specification.

When an HTTP/2 server sends a GOAWAY frame:
1. The connection is marked for closure
2. Java's HttpClient detects this and opens a new connection
3. Subsequent requests succeed by using the new connection
4. This behavior is **correct and resilient**

The p2 issue referenced (https://github.com/eclipse-equinox/p2/issues/529) is likely related to:
- Retry logic and error handling during long-running downloads
- Specific timing issues when GOAWAY occurs mid-download
- Connection pool management in specific scenarios
- Older Java versions or different HTTP client implementations

## Test Scenarios

### 1. Normal HTTP/2 Request (`testNormalHttp2Request`)
Tests that basic HTTP/2 communication works without GOAWAY. ✅ PASSES

### 2. GOAWAY After First Request (`testGoawayAfterFirstRequest`)
Demonstrates that when the server sends GOAWAY after processing one request, the Java HttpClient successfully handles it by opening a new connection for the second request. ✅ PASSES

### 3. Multiple Requests With GOAWAY (`testMultipleRequestsWithGoaway`)
Simulates a scenario where several requests succeed, then GOAWAY is sent. The HttpClient successfully continues with a new connection. ✅ PASSES

### 4. Immediate GOAWAY (`testImmediateGoaway`)
Most aggressive scenario where the server sends GOAWAY immediately upon connection. The HttpClient successfully retries with a new connection. ✅ PASSES

## Architecture

### Server Components

- **Http2ServerWithGoaway**: Main server class that manages the Netty server lifecycle and provides GOAWAY control
- **Http2ServerInitializer**: Sets up the HTTP/2 codec and handlers
- **Http2ServerHandler**: Handles HTTP/2 frames and sends GOAWAY on demand based on configuration

### Test Client

The tests use Java 11's `HttpClient` with HTTP/2 version explicitly specified. The client is configured to:
- Use HTTP/2 protocol (via `HttpClient.Version.HTTP_2`)
- Accept self-signed certificates (for testing purposes)
- Have appropriate timeouts

## Test Results

All tests **PASS**, demonstrating that:
- Java HttpClient resilient handles GOAWAY frames
- New connections are automatically established when GOAWAY is received
- The HTTP/2 implementation in Java 11+ is robust

## HTTP/2 GOAWAY Frame

According to [RFC 7540 Section 6.8](https://httpwg.org/specs/rfc7540.html#GOAWAY), GOAWAY frames are used to initiate shutdown of a connection or signal serious error conditions. The frame allows an endpoint to gracefully stop accepting new streams while letting previously established streams complete.

## Usage for Further Testing

This project can be used to:
1. Test different retry strategies in client code
2. Evaluate connection pool behavior under GOAWAY scenarios
3. Test custom error handlers
4. Develop and verify fixes for specific GOAWAY-related issues
5. Benchmark performance impact of GOAWAY handling

## Running Individual Tests

```bash
# Run specific test
mvn test -Dtest=Http2GoawayTest#testGoawayAfterFirstRequest

# Run with debug output
mvn test -X
```

## Related Issues

- https://github.com/eclipse-equinox/p2/issues/529 - Original p2 issue about GOAWAY handling
- [RFC 7540 Section 6.8](https://httpwg.org/specs/rfc7540.html#GOAWAY) - HTTP/2 GOAWAY frame specification

## Conclusion

This test project successfully demonstrates HTTP/2 GOAWAY behavior and confirms that Java 11's HttpClient handles GOAWAY correctly. The test infrastructure is now available for further investigation of edge cases and specific scenarios that may cause issues in downstream projects.
