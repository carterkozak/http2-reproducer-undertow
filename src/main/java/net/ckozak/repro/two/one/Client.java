package net.ckozak.repro.two.one;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import com.palantir.conjure.java.api.config.service.UserAgent;
import com.palantir.conjure.java.api.config.ssl.SslConfiguration;
import com.palantir.conjure.java.client.config.ClientConfiguration;
import com.palantir.conjure.java.client.config.ClientConfigurations;
import com.palantir.conjure.java.client.jaxrs.JaxRsClient;
import com.palantir.conjure.java.config.ssl.SslSocketFactories;
import com.palantir.conjure.java.okhttp.HostMetricsRegistry;
import net.ckozak.repro.LoggerBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class Client {
    static {
        LoggerBindings.initialize();
    }
    private static final Logger log = LoggerFactory.getLogger(Client.class);
    private static final int PORT = 8443;
    private static final int THREADS = 32;
    private static final AtomicLong sent = new AtomicLong();
    private static final AtomicLong success = new AtomicLong();

    public static void main(String[] args) {
        SslConfiguration sslConfig = SslConfiguration.of(Paths.get("src/main/resources/trustStore.jks"));
        // This client uses an older okhttp version with http/2 bugs. While it doesn't represent ideal state,
        // the server should gracefully handle incorrect inputs.
        SimpleService client = JaxRsClient.create(
                SimpleService.class,
                UserAgent.of(UserAgent.Agent.of("repro", "0.0.1")),
                new HostMetricsRegistry(),
                ClientConfiguration.builder()
                        .from(ClientConfigurations.of(
                                ImmutableList.of("https://localhost:" + PORT),
                                SslSocketFactories.createSslSocketFactory(sslConfig),
                                SslSocketFactories.createX509TrustManager(sslConfig)))
                        .enableGcmCipherSuites(true)
                        .backoffSlotSize(Duration.ZERO)
                        .maxNumRetries(0)
                        .clientQoS(ClientConfiguration.ClientQoS.DANGEROUS_DISABLE_SYMPATHETIC_CLIENT_QOS)
                        .build());

        ExecutorService executor = Executors.newCachedThreadPool();
        List<Thread> threads = new CopyOnWriteArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            executor.execute(() -> {
                threads.add(Thread.currentThread());
                while (true) {
                    sent.incrementAndGet();
                    // reset interruption
                    Thread.interrupted();
                    try {
                        client.ping();
                        success.incrementAndGet();
                    } catch (RuntimeException e) {
                        // interruption cancels requests, which can fail in interesting ways upon interruption.
                        // We're interested in how the server responds in these cases.
                        String message = e.getMessage();
                        if (message == null || !message.contains("cancelled via interruption")) {
                            log.warn("client failure", e);
                        }
                    }
                }
            });
        }
        int iterations = 0;
        while (true) {
            iterations++;
            if (iterations % 100 == 0) {
                log.info("Total client requests: {} successful requests: {}", sent.get(), success.get());
            }
            // Depending on hardware, the delay duration may need to scale up or down. If too few requests are able to
            // successfully complete, we don't get a good distribution of interruptions across the request lifespan
            // and fail to reproduce bugs, but if too many complete successfully there are too few opportunities
            // to trigger the race.
            Uninterruptibles.sleepUninterruptibly(Duration.ofMillis(1));
            Thread randomThread = threads.get(ThreadLocalRandom.current().nextInt(threads.size()));
            randomThread.interrupt();
        }
    }

    @Path("/simple")
    @Produces("application/json")
    @Consumes("application/json")
    public interface SimpleService {

        @POST
        @Path("/ping")
        String ping();

    }
}