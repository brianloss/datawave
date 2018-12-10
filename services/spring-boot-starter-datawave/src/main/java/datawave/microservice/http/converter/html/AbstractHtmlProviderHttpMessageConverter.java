package datawave.microservice.http.converter.html;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

abstract public class AbstractHtmlProviderHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {
    public AbstractHtmlProviderHttpMessageConverter() {
        setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_HTML));
    }
    
    @Override
    protected boolean canRead(MediaType mediaType) {
        return false;
    }
    
    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
        return null;
    }
    
    @Override
    protected void writeInternal(T t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        outputMessage.getBody().write(createHtml(t));
    }
    
    public abstract String getTitle(T t);
    
    public abstract String getHeadContent(T t);
    
    public abstract String getPageHeader(T t);
    
    public abstract String getMainContent(T t);
    
    protected byte[] createHtml(T t) {
        
        String html = "<html>" + "<head>" + "<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"/>" + "<title>" + "DATAWAVE - " + getTitle(t)
                        + "</title>" + "<link rel='stylesheet' type='text/css' href='/screen.css' media='screen' />" + getHeadContent(t) + "</head>" + "<body>"
                        + "<h1>" + getPageHeader(t) + "</h1>" + "<div>" + getMainContent(t) + "</div>" + "<br/>" + "</body></html>\n";
        return html.getBytes(StandardCharsets.UTF_8);
    }
}
