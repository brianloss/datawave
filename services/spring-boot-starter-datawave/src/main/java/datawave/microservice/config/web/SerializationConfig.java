package datawave.microservice.config.web;

import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SerializationConfig {
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
}
