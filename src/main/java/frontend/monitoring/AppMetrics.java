package frontend.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AppMetrics {

    private final MeterRegistry registry;
    private final String appVersion;

    private Counter predictionSuccess;
    private Counter predictionError;
    private Timer predictionLatency;

    private final AtomicInteger activeUsers = new AtomicInteger(0);

    public AppMetrics(MeterRegistry registry, Environment env) {
        this.registry = registry;
        // Get version from APP_VERSION environment variable (set in deployment)
        this.appVersion = env.getProperty("APP_VERSION", "v1");
    }

    @PostConstruct
    public void init() {
        predictionSuccess = Counter.builder("app_predictions_total")
                .description("Total number of prediction requests")
                .tag("source", "frontend")
                .tag("status", "success")
                .tag("version", appVersion)
                .register(registry);

        predictionError = Counter.builder("app_predictions_total")
                .description("Total number of prediction requests")
                .tag("source", "frontend")
                .tag("status", "error")
                .tag("version", appVersion)
                .register(registry);

        predictionLatency = Timer.builder("app_prediction_latency_seconds")
                .description("Prediction latency in seconds (frontend)")
                .tag("source", "frontend")
                .tag("version", appVersion)
                .publishPercentileHistogram()
                .register(registry);

        Gauge.builder("app_active_users", activeUsers::get)
                .description("Active users on home page")
                .tag("page", "home")
                .tag("version", appVersion)
                .register(registry);
    }

    public void recordSuccess(long durationMillis) {
        predictionSuccess.increment();
        predictionLatency.record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public void recordError(long durationMillis) {
        predictionError.increment();
        predictionLatency.record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public void userVisitedHome() {
        activeUsers.incrementAndGet();
    }

    public void userLeftHome() {
        activeUsers.decrementAndGet();
    }
}
