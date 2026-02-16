package in.lifehive.grafana_prometheus_loki.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/grafana-prometheus-loki")
@EnableAsync
public class MetricsController {

    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    private final Random random = new Random();
    private final Counter slowApiFailureCounter;
    private final Timer slowApiTimer;

    public MetricsController(MeterRegistry meterRegistry) {
        this.slowApiFailureCounter = Counter.builder("demo_slow_api_failures_total")
                .description("Number of times /slow API failed with 500")
                .register(meterRegistry);
        this.slowApiTimer = Timer.builder("demo_slow_api_duration")
                .description("Time taken by /slow API")
                .register(meterRegistry);
    }

    @GetMapping("/hello")
    public String hello() {
        logger.info("Hit /hello endpoint");
        return "Hello from Spring Boot!";
    }

    @GetMapping("/slow")
    @Async
    public CompletableFuture<ResponseEntity<String>> slowApi() {
        return CompletableFuture.supplyAsync(() ->
            slowApiTimer.record(() -> {
                try {
                    int delay = 500 + random.nextInt(3000);
                    logger.info("Slow API called. Sleeping for {} ms", delay);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                boolean fail = random.nextBoolean();
                if(fail) {
                    logger.warn("Slow API failed randomly with 500 error");
                    slowApiFailureCounter.increment();
                    return ResponseEntity.status(500).body("Random failure happened!");
                } else {
                    logger.info("Slow API succeeded");
                    return ResponseEntity.ok("Slow API success response!");
                }
                    }
            ));
    }

    @GetMapping("/log")
    public String logSomething() {
        logger.warn("THis is a demo WARN log - just checking logging!");
        return "Logged a WARN message. Check your logs!";
    }
}
