package datawave.microservice.http.converter.html;

import datawave.webservice.query.exception.QueryExceptionType;
import datawave.webservice.result.VoidResponse;

import java.util.List;

/**
 * A {@link org.springframework.http.converter.HttpMessageConverter} that writes a {@link VoidResponse} to HTML. This class does not support reading HTML and
 * converting to an {@link VoidResponse}.
 */
public class VoidResponseHttpMessageConverter extends AbstractHtmlProviderHttpMessageConverter<VoidResponse> {
    
    @Override
    protected boolean supports(Class<?> clazz) {
        return VoidResponse.class.isAssignableFrom(clazz);
    }
    
    @Override
    public String getTitle(VoidResponse voidResponse) {
        return "Void Response";
    }
    
    @Override
    public String getHeadContent(VoidResponse voidResponse) {
        return "";
    }
    
    @Override
    public String getPageHeader(VoidResponse voidResponse) {
        return VoidResponse.class.getName();
    }
    
    public static final String BR = "<br/>";
    
    @Override
    public String getMainContent(VoidResponse voidResponse) {
        StringBuilder builder = new StringBuilder();
        builder.append("<b>MESSAGES:</b>").append(BR);
        List<String> messages = voidResponse.getMessages();
        if (messages != null) {
            for (String msg : messages) {
                if (msg != null)
                    builder.append(msg).append(BR);
            }
        }
        
        builder.append("<b>EXCEPTIONS:</b>").append(BR);
        List<QueryExceptionType> exceptions = voidResponse.getExceptions();
        if (exceptions != null) {
            for (QueryExceptionType exception : exceptions) {
                if (exception != null)
                    builder.append(exception).append(", ").append(QueryExceptionType.getSchema()).append(BR);
            }
        }
        return builder.toString();
    }
}
