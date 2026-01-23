# App

The frontend allows users to interact with the model in the backend through a web-based UI.

The frontend is implemented with Spring Boot and only consists of a website and one REST endpoint.
It **requires Java 25+** to run (tested with 25.0.1).
Any classification requests will be delegated to the `backend` service that serves the model.
You must specify the environment variable `MODEL_HOST` to define where the backend is running.

The frontend service can be started through running the `Main` class (e.g., in your IDE) or through Maven (recommended):

    MODEL_HOST="http://localhost:8081" mvn spring-boot:run

The server runs on port 8080. Once its startup has finished, you can access [localhost:8080/sms](http://localhost:8080/sms) in your browser to interact with the application.

## Metrics

The frontend exposes Prometheus metrics at `/actuator/prometheus` for monitoring and observability.

### Verify Metrics Locally

Start the application and test the metrics endpoint:

```bash
# Start the frontend
MODEL_HOST="http://localhost:8081" mvn spring-boot:run

# In another terminal, check metrics
curl http://localhost:8080/actuator/prometheus | grep app_
```

### Custom Metrics Exposed

The application exposes the following custom business metrics:

- **`app_predictions_total{source="frontend",status="success|error"}`** (Counter)
  Total number of prediction requests, labeled by status (success or error)

- **`app_prediction_latency_seconds{source="frontend"}`** (Histogram)
  Prediction request duration in seconds, with percentile histograms (p50, p75, p95, p99)

- **`app_active_users{page="home"}`** (Gauge)
  Number of active users currently on the home page

These metrics are automatically scraped by Prometheus when deployed to Kubernetes via ServiceMonitor.
