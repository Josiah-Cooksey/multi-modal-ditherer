package dev.jcooksey.lambda;

import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.*;

import org.eclipse.jetty.http.MultiPart.Parser;
import org.eclipse.jetty.http.MultiPart.Part;

public class DitherRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>
{
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context)
    {
        final LambdaLogger logger = context.getLogger();
        logger.log("Headers: " + event.getHeaders());
        logger.log("Body: " + event.getBody());
        logger.log("IsBase64Encoded: " + event.getIsBase64Encoded());

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);


        if (!event.getIsBase64Encoded())
        {
            response.setBody("{\"error\": \"Input image/form was not Base64 encoded.\"}");
            response.setStatusCode(400);
            return response;
        }
        if (!event.getHeaders().containsKey("content-type") || !event.getHeaders().get("content-type").equalsIgnoreCase("multipart/form-data"))
        {
            response.setBody("{\"error\": \"POSTs must be multipart/form-data.\"}");
            response.setStatusCode(400);
            return response;
        }

        byte[] formData = Base64.getDecoder().decode(event.getBody());

        // for now, I'll just return the input image
        response.setBody(event.getBody());
        response.setStatusCode(200);
        return response;



        // TODO: use Apache Commons FileUpload library or jetty to parse the body of the request (multipart/form-data)
        // ✓ it needs to be converted from Base64 into binary, then parsed into the constituent form fields
        // afterwards, we need to validate that the expected data was uploaded
        // if it's valid, we can perform the dithering
        // finally, for this lambda module specifically, we need to package the data to return it
        // I'd like to use json formatting because the website can then handle the response in a more standard way
        // but the downside is that we need to encode it as base64, which uses more processing time both in this back-end as well as on the front-end
        // instead, it may be possible to return a image/png response for valid requests but a json response for any issues
        // that way, the front-end doesn't need to perform much extra work other than determining the response type and displaying the appropriate image or error message
    }
}