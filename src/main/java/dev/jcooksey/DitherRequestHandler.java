package dev.jcooksey;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.Map;

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

        if (event.getIsBase64Encoded())
        {
            // for now, I'll just return the input image
            response.setBody(event.getBody());
            response.setStatusCode(200);
            return response;
        }

        response.setBody("{\"error\": \"Input image was not Base64 encoded.\"}");
        response.setStatusCode(400);
        return response;
    }
}