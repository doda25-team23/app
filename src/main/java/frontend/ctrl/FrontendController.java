package frontend.ctrl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import frontend.monitoring.AppMetrics;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import frontend.data.Sms;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/sms")
public class FrontendController {

    private String modelHost;

    private RestTemplateBuilder rest;

    private final AppMetrics metrics;

    public FrontendController(RestTemplateBuilder rest, Environment env, AppMetrics metrics) {
        this.rest = rest;
        this.metrics = metrics;
        this.modelHost = env.getProperty("MODEL_HOST");
        assertModelHost();
    }


    private void assertModelHost() {
        if (modelHost == null || modelHost.strip().isEmpty()) {
            System.err.println("ERROR: ENV variable MODEL_HOST is null or empty");
            System.exit(1);
        }
        modelHost = modelHost.strip();
        if (modelHost.indexOf("://") == -1) {
            var m = "ERROR: ENV variable MODEL_HOST is missing protocol, like \"http://...\" (was: \"%s\")\n";
            System.err.printf(m, modelHost);
            System.exit(1);
        } else {
            System.out.printf("Working with MODEL_HOST=\"%s\"\n", modelHost);
        }
    }

    @GetMapping("")
    public String redirectToSlash(HttpServletRequest request) {
        // relative REST requests in JS will end up on / and not on /sms
        return "redirect:" + request.getRequestURI() + "/";
    }

    @GetMapping("/")
    public String index(Model m) {
        metrics.userVisitedHome();
        m.addAttribute("hostname", modelHost);
        return "sms/index";
    }


    @PostMapping("/")
    public String index(Model m, @RequestParam("sms") String sms) {
        long start = System.currentTimeMillis();
        try {
            // existing code calling model-service
            String url = modelHost + "/predict";
            var response = rest.build().postForObject(url, Map.of("sms", sms), Map.class);

            long duration = System.currentTimeMillis() - start;
            metrics.recordSuccess(duration);

            m.addAttribute("result", response.get("result"));
            return "sms/index";
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            metrics.recordError(duration);
            throw e;
        }
    }


    private String getPrediction(Sms sms) {
        try {
            var url = new URI(modelHost + "/predict");
            var c = rest.build().postForEntity(url, sms, Sms.class);
            return c.getBody().result.trim();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}