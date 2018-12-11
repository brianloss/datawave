package datawave.microservice.config.web;

import com.google.common.collect.Lists;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Validated
@EnableConfigurationProperties(DatawaveServerProperties.class)
@ConfigurationProperties(prefix = "server", ignoreInvalidFields = true)
public class DatawaveServerProperties {
    @PositiveOrZero
    private Integer nonSecurePort = null;
    
    @NotBlank
    private String cssUri = "/screen.css";
    
    @Valid
    @NestedConfigurationProperty
    private Cors cors = new Cors();
    
    public Integer getNonSecurePort() {
        return nonSecurePort;
    }
    
    public void setNonSecurePort(Integer nonSecurePort) {
        this.nonSecurePort = nonSecurePort;
    }
    
    public String getCssUri() {
        return cssUri;
    }
    
    public void setCssUri(String cssUri) {
        this.cssUri = cssUri;
    }
    
    public Cors getCors() {
        return cors;
    }
    
    public static class Cors {
        @NotEmpty
        private List<String> corsPaths = Lists.newArrayList("/**");
        @NotEmpty
        private String[] allowedOrigins = new String[] {"*"};
        @NotEmpty
        private String[] allowedMethods = new String[] {"HEAD", "DELETE", "GET", "POST", "PUT", "OPTIONS"};
        private boolean allowCredentials = true;
        @NotEmpty
        private String[] allowedHeaders = new String[] {"X-SSL-clientcert-subject", "X-SSL-clientcert-issuer", "X-ProxiedEntitiesChain",
                "X-ProxiedIssuersChain", "Accept", "Accept-Encoding"};
        @Positive
        private long maxAge = TimeUnit.DAYS.toSeconds(10);
        
        public List<String> getCorsPaths() {
            return corsPaths;
        }
        
        public void setCorsPaths(List<String> corsPaths) {
            this.corsPaths = corsPaths;
        }
        
        public String[] getAllowedOrigins() {
            return allowedOrigins;
        }
        
        public void setAllowedOrigins(String[] allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
        
        public String[] getAllowedMethods() {
            return allowedMethods;
        }
        
        public void setAllowedMethods(String[] allowedMethods) {
            this.allowedMethods = allowedMethods;
        }
        
        public String[] getAllowedHeaders() {
            return allowedHeaders;
        }
        
        public void setAllowedHeaders(String[] allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }
        
        public boolean isAllowCredentials() {
            return allowCredentials;
        }
        
        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }
        
        public long getMaxAge() {
            return maxAge;
        }
        
        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }
}
