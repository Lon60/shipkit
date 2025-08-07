package io.shipkit.gatewayapi.gatewayapi.core.config;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class K8sTemplateRendererTest {

    private Configuration cfg() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setNumberFormat("computer");
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        return cfg;
    }

    @Test
    void failsWhenPlaceholderMissing() {
        K8sTemplateRenderer renderer = new K8sTemplateRenderer(cfg());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                renderer.render("namespace.ftl.yaml", Map.of())
        );
        assertTrue(ex.getMessage().contains("missing key"));
    }

    @Test
    void rendersWhenAllPlaceholdersProvided() {
        K8sTemplateRenderer renderer = new K8sTemplateRenderer(cfg());
        String out = renderer.render("namespace.ftl.yaml", Map.of("namespace", "test-ns"));
        assertTrue(out.contains("name: test-ns"));
    }

    @Test
    void ingressRouteRequiresMiddlewaresKeyEvenIfEmpty() {
        K8sTemplateRenderer renderer = new K8sTemplateRenderer(cfg());
        Map<String, Object> model = Map.of(
                "name", "shipkit-frontend",
                "namespace", "shipkit-system",
                "sslEnabled", false,
                "match", "Host(`shipkit.local`) && PathPrefix(`/`)",
                "serviceName", "shipkit-frontend",
                "servicePort", 3000,
                "serviceNamespace", "shipkit-system",
                "middlewares", java.util.List.of()
        );
        String out = renderer.render("ingressroute.ftl.yaml", model);
        assertTrue(out.contains("kind: IngressRoute"));
        assertFalse(out.contains(","));
    }
}


