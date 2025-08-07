package io.shipkit.gatewayapi.gatewayapi.core.config;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class K8sTemplateRenderer {

    private final freemarker.template.Configuration freemarkerConfig;

    public String render(String templatePath, Map<String, Object> model) {
        try {
            Template template = freemarkerConfig.getTemplate(templatePath);
            String templateSource = readTemplateSource(templatePath);
            validatePlaceholders(templateSource, model.keySet());
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render template '" + templatePath + "': " + e.getMessage(), e);
        }
    }

    private String readTemplateSource(String templatePath) throws Exception {
        String resourcePath = "templates/" + templatePath;
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Template not found on classpath: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void validatePlaceholders(String templateSource, Set<String> providedKeys) {
        Set<String> referenced = new java.util.HashSet<>();
        Set<String> listAliases = new java.util.HashSet<>();
        Set<String> optionalKeys = new java.util.HashSet<>();

        // ${var}
        Matcher m1 = Pattern.compile("\\$\\{([a-zA-Z0-9_./-]+)\\}").matcher(templateSource);
        while (m1.find()) {
            referenced.add(m1.group(1));
        }

        // <#if var> (simple var condition)
        Matcher m2 = Pattern.compile("<#if\\s+([a-zA-Z0-9_./-]+)\\s*>").matcher(templateSource);
        while (m2.find()) {
            referenced.add(m2.group(1));
        }

        // <#if var?? ...> marks 'var' as optional
        Matcher m2opt = Pattern.compile("<#if\\s+([a-zA-Z0-9_./-]+)\\?\\?").matcher(templateSource);
        while (m2opt.find()) {
            optionalKeys.add(m2opt.group(1));
        }

        // <#list collection as item>
        Matcher m3 = Pattern.compile("<#list\\s+([a-zA-Z0-9_./-]+)\\s+as\\s+([a-zA-Z0-9_./-]+)>").matcher(templateSource);
        while (m3.find()) {
            referenced.add(m3.group(1));
            listAliases.add(m3.group(2));
        }

        // Fail if template references keys that were not provided
        for (String key : referenced) {
            String root = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
            if (listAliases.contains(root)) {
                continue;
            }
            if (optionalKeys.contains(root)) {
                // optional: only require if provided
                if (providedKeys.contains(root) && !providedKeys.contains(key)) {
                    throw new IllegalStateException("Template expects missing key: " + key);
                }
                continue;
            }
            if (!providedKeys.contains(key) && !providedKeys.contains(root)) {
                throw new IllegalStateException("Template expects missing key: " + key);
            }
        }

        // Fail if code provides keys that the template does not reference
        for (String provided : providedKeys) {
            if (!referenced.contains(provided) && !optionalKeys.contains(provided)) {
                throw new IllegalStateException("Provided key not used by template: " + provided);
            }
        }
    }
}


