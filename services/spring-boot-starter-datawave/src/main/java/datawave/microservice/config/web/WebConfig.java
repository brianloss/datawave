package datawave.microservice.config.web;

import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import datawave.microservice.config.web.DatawaveServerProperties.Cors;
import datawave.microservice.http.converter.html.HtmlProviderHttpMessageConverter;
import datawave.microservice.http.converter.html.VoidResponseHttpMessageConverter;
import datawave.microservice.http.converter.protostuff.ProtostuffHttpMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig {
    /**
     * Creates a {@link JaxbAnnotationModule} bean, which will be added automatically to any {@link com.fasterxml.jackson.databind.ObjectMapper} created by
     * Spring.
     * 
     * @return a new {@link JaxbAnnotationModule}
     */
    @Bean
    public JaxbAnnotationModule jaxbAnnotationModule() {
        return new JaxbAnnotationModule();
    }
    
    @Bean
    public WebMvcConfigurer corsConfiguration(final DatawaveServerProperties serverProperties) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                Cors cors = serverProperties.getCors();
                cors.getCorsPaths().forEach(p -> {
                    registry.addMapping(p).allowedOrigins(cors.getAllowedOrigins()).allowedMethods(cors.getAllowedMethods())
                                    .allowedHeaders(cors.getAllowedHeaders()).allowCredentials(cors.isAllowCredentials()).maxAge(cors.getMaxAge());
                });
            }
        };
    }
    
    @Bean
    public WebMvcConfigurer messageConverterConfiguration(final DatawaveServerProperties serverProperties) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new ProtostuffHttpMessageConverter());
                converters.add(new VoidResponseHttpMessageConverter(serverProperties));
                converters.add(new HtmlProviderHttpMessageConverter(serverProperties));
            }
        };
    }
}
