package net.ckozak.repro.one;

import com.google.common.io.ByteStreams;
import com.palantir.conjure.java.api.config.ssl.SslConfiguration;
import com.palantir.conjure.java.config.ssl.SslSocketFactories;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Protocols;
import net.ckozak.repro.LoggerBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

public final class Server {
    static {
        LoggerBindings.initialize();
    }

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws Exception {
        SSLContext sslContext = SslSocketFactories.createSslContext(SslConfiguration.of(
                Paths.get("src/main/resources/trustStore.jks"),
                Paths.get("src/main/resources/keyStore.jks"),
                "keystore"));
        AtomicLong requests = new AtomicLong();
        Undertow server = Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .addHttpsListener(8443, null, sslContext)
                .setHandler(new BlockingHandler(exchange -> {
                    long current = requests.incrementAndGet();
                    if (current % 1000 == 0) {
                        log.info("Received {} requests", current);
                    }
                    if (!Protocols.HTTP_2_0.equals(exchange.getProtocol())) {
                        log.error("Bad protocol: {}", exchange.getProtocol());
                    }
                    ByteStreams.copy(exchange.getInputStream(), NilOutputStream.INSTANCE);
                }))
                .build();
        server.start();
    }

    private static final class NilOutputStream extends OutputStream {
        private static final OutputStream INSTANCE = new NilOutputStream();

        @Override
        public void write(int b) throws IOException {}

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {}
    }
}
