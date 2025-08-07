package io.shipkit.gatewayapi.gatewayapi.core.config;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@org.springframework.context.annotation.Configuration
public class FreemarkerConfig {

    @Bean
    @Primary
    public Configuration freemarkerConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setNumberFormat("computer");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        return cfg;
    }
}


