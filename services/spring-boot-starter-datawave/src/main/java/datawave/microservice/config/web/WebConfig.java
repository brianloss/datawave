package datawave.microservice.config.web;

import datawave.microservice.http.converter.html.HtmlProviderHttpMessageConverter;
import datawave.microservice.http.converter.html.VoidResponseHttpMessageConverter;
import datawave.microservice.http.converter.protostuff.ProtostuffHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

@Configuration
public class WebConfig extends WebMvcConfigurationSupport {
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ProtostuffHttpMessageConverter());
        converters.add(new VoidResponseHttpMessageConverter());
        converters.add(new HtmlProviderHttpMessageConverter());
    }
}
