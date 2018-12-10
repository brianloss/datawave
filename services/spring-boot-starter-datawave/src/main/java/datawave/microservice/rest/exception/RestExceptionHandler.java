package datawave.microservice.rest.exception;

import datawave.webservice.query.exception.QueryException;
import datawave.webservice.result.VoidResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

@ControllerAdvice
@ConditionalOnClass(QueryException.class)
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        System.out.println("****** HANDLING EXCEPTION " + ex.getMessage() + " / " + ex.getClass());
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }
        VoidResponse vr = new VoidResponse();
        vr.addException(ex);
        return new ResponseEntity<>(vr, headers, status);
    }
    
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status,
                    WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), headers, status, request);
    }
    
    @ExceptionHandler({QueryException.class})
    protected ResponseEntity<Object> handleQueryException(QueryException e, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return handleExceptionInternal(e, null, new HttpHeaders(), status, request);
    }
}
