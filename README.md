# Reproducer for Undertow http/2 premature cancellation race conditions

Note: these are meant primarily to diagnose issues using HTTP/2, so they should be run using jdk9+.
I have successfully reproduced the following issues using openjdk 11 and zulu 11:

https://issues.redhat.com/browse/UNDERTOW-1638
https://issues.redhat.com/browse/UNDERTOW-1639
https://issues.redhat.com/browse/UNDERTOW-1645
https://issues.redhat.com/browse/UNDERTOW-1643
https://issues.redhat.com/browse/UNDERTOW-1640

Depending on hardware, the sleep duration between interruptions and request/response body sizes may
need to be modified.

## IDE configuration

Using intellij idea, gradle integration works well. Alternatively `./gradlew idea` can be used, which requires the
`http2-reproducer-undertow.ipr` file to be opened rather than the project directory.

Eclipse users can run `./gradlew eclipse` to generate an eclipse project.

## One

Reproducer for cancellations while sending data to the server.

## Two

Reproducer for cancellations while responding to a client