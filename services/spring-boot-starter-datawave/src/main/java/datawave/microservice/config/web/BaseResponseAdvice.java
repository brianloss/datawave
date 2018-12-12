package datawave.microservice.config.web;

import datawave.webservice.result.BaseResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class BaseResponseAdvice implements ResponseBodyAdvice<BaseResponse> {
    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    @Override
    public BaseResponse beforeBodyWrite(BaseResponse baseResponse, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        if (serverHttpRequest instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest sr = (ServletServerHttpRequest) serverHttpRequest;
            Long startTime = (Long) sr.getServletRequest().getAttribute("X-OperationStartTimeInMS");
            if (startTime != null) {
                long operationTime = System.currentTimeMillis() - startTime;
                baseResponse.setOperationTimeMS(operationTime);
                serverHttpResponse.getHeaders().set("X-OperationTimeInMS", Long.toString(operationTime));
            }
        }
        return baseResponse;
    }
}
