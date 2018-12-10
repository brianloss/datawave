package datawave.microservice.dictionary;

import datawave.webservice.query.exception.NoResultsQueryException;
import datawave.webservice.query.exception.QueryException;
import datawave.webservice.result.VoidResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static datawave.microservice.http.converter.protostuff.ProtostuffHttpMessageConverter.PROTOSTUFF_VALUE;

@RestController
@RequestMapping(path = "/v1", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, PROTOSTUFF_VALUE})
public class DataDictionaryOperations {
    
    @ResponseBody
    @RequestMapping(path = "/void")
    public VoidResponse testVoidResponse(@RequestParam boolean fail) throws QueryException {
        if (fail) {
            throw new NoResultsQueryException("There are no results!");
        }
        VoidResponse vr = new VoidResponse();
        vr.setHasResults(true);
        vr.setMessages(Arrays.asList("msg1", "msg2", "This is a longer message."));
        vr.addException(new QueryException("This is a test exception"));
        vr.setOperationTimeMS(3);
        return vr;
    }
}
