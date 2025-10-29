import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import dev.jcooksey.lambda.DitherRequestHandler;

public class EventTest
{
    public static void main(String[] args)
    {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        String longString = "";
        try
        {
            longString = Files.readString(Path.of("src/test/resources/requestBody.txt"));
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        event.setBody(longString);
        event.setIsBase64Encoded(true);
        event.setHeaders(Map.ofEntries(
                Map.entry("accept", "text/html,application/xhtml+xml,application/xml"),
                Map.entry("application", "text/html"),
                Map.entry("content-length", "70829"),
                Map.entry("content-type", "multipart/form-data"),
                Map.entry("boundary", "----geckoformboundary730a031b35ce426e73706e0de46ba922"),
                Map.entry("host", "api.jcooksey.dev")
        ));
        Context context = new Context()
        {
            LambdaLogger lambdaLogger = new LambdaLogger()
            {
                @Override
                public void log(String s)
                {
                    System.out.println(s);
                }

                @Override
                public void log(byte[] bytes)
                {

                }
            };
            @Override
            public String getAwsRequestId()
            {
                return "";
            }

            @Override
            public String getLogGroupName()
            {
                return "";
            }

            @Override
            public String getLogStreamName()
            {
                return "";
            }

            @Override
            public String getFunctionName()
            {
                return "";
            }

            @Override
            public String getFunctionVersion()
            {
                return "";
            }

            @Override
            public String getInvokedFunctionArn()
            {
                return "";
            }

            @Override
            public CognitoIdentity getIdentity()
            {
                return null;
            }

            @Override
            public ClientContext getClientContext()
            {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis()
            {
                return 0;
            }

            @Override
            public int getMemoryLimitInMB()
            {
                return 0;
            }

            @Override
            public LambdaLogger getLogger()
            {
                return lambdaLogger;
            }
        };

        DitherRequestHandler ditherer = new DitherRequestHandler();
        ditherer.handleRequest(event, context);
        System.out.println("Done!");
    }
}
