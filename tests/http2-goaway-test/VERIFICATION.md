# Verification Guide for GOAWAY Fix

## What Was Fixed

The GOAWAY frames were not being sent because each HTTP/2 connection creates a new `Http2ServerHandler` instance via Netty's `ChannelInitializer`, but the configuration methods (`setGoawayAfterRequests`, `setSendGoawayImmediately`) were only updating the first handler instance created during server initialization, not the handlers that actually process requests.

## Solution

1. Moved configuration storage from handler instances to the server class
2. Pass configuration to each new handler via constructor
3. Added extensive logging to track GOAWAY sending

## How to Verify

### 1. Run Tests
```bash
cd tests/http2-goaway-test
mvn clean test
```

**Expected output:**
```
Processing request #1
Sending GOAWAY frame to client
GOAWAY frame sent successfully
✓ First request completed, server will send GOAWAY after this
...
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 2. Debug with Breakpoints

Set breakpoint in:
- `jdk.internal.net.http.Http2Connection.handleConnectionFrame(Http2Frame)` - to see GOAWAY received
- `org.eclipse.ecf.tests.http2.goaway.Http2ServerHandler.sendGoaway(ChannelHandlerContext)` - to see GOAWAY sent

Run test in debug mode:
```bash
mvn test -Dmaven.surefire.debug
```

Connect debugger to port 5005.

### 3. Check Client Logs

The test already enables HTTP/2 client logging via:
```java
System.setProperty("jdk.httpclient.HttpClient.log", "all");
```

Look for these messages in output:
- `"Shutting down connection: Idle connection closed by HTTP/2 peer"` - GOAWAY received
- `"FRAME: IN: GOAWAY"` - GOAWAY frame received (if present)

### 4. Server Logs

Server now logs each step:
- `"Processing request #N"` - Request received
- `"Request count reached N, sending GOAWAY"` - Threshold reached
- `"Sending GOAWAY frame to client"` - About to send
- `"GOAWAY frame sent successfully"` - Sent and flushed

## Test Scenarios

1. **testNormalHttp2Request**: Baseline - no GOAWAY (✅ passes)
2. **testGoawayAfterFirstRequest**: GOAWAY after 1 request (✅ passes, logs show GOAWAY sent)
3. **testMultipleRequestsWithGoaway**: GOAWAY after 2 requests (✅ passes, logs show GOAWAY sent)
4. **testImmediateGoaway**: GOAWAY on connection (✅ passes, logs show GOAWAY sent)

## Key Changes in Code

### Http2ServerWithGoaway.java
- Added fields: `sendGoawayImmediately`, `goawayAfterRequests`
- Pass config to handler constructor: `new Http2ServerHandler(sendGoawayImmediately, goawayAfterRequests)`
- Removed `handler` field (no longer needed)

### Http2ServerHandler.java
- Constructor now takes config: `Http2ServerHandler(boolean sendGoawayImmediately, int goawayAfterRequests)`
- Added logging throughout
- Removed setter methods (config is immutable per handler)

## What You Should See

When setting breakpoint in `jdk.internal.net.http.Http2Connection.handleConnectionFrame`:
1. The method will be called with a GOAWAY frame
2. The switch statement will hit the `GOAWAY` case
3. Connection shutdown will be initiated

The breakpoint should hit when running `testGoawayAfterFirstRequest` or `testMultipleRequestsWithGoaway` tests.
