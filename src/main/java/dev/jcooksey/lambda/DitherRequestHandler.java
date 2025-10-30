package dev.jcooksey.lambda;

import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import dev.jcooksey.core.Ditherer;
import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.io.Content;

import javax.imageio.ImageIO;

public class DitherRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>
{
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context)
    {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("content-type", "application/json");
        responseHeaders.put("access-control-allow-origin", "https://jcooksey.dev");
        response.setHeaders(responseHeaders);

        final LambdaLogger logger = context.getLogger();
        logger.log("Headers: " + event.getHeaders());
        logger.log("Body: " + event.getBody());
        logger.log("IsBase64Encoded: " + event.getIsBase64Encoded());
        String boundary;
        try
        {
            boundary = validateHeaders(event, logger);
        }
        catch (FormValidationException e)
        {
            response.setStatusCode(400);
            response.setBody("{\"error\": \"" + e.getMessage() + "\"}");
            return response;
        }

        byte[] formData = Base64.getDecoder().decode(event.getBody());
        logger.log(new String(formData, 0, 200, StandardCharsets.UTF_8));
        FormListener formListener = new FormListener(3);
        MultiPart.Parser formParser = new MultiPart.Parser(boundary, formListener);

        try
        {
            Content.Chunk chunk = Content.Chunk.from(ByteBuffer.wrap(formData), true);
            formParser.parse(chunk);
        }
        catch (RuntimeException e)
        {
            response.setStatusCode(400);
            response.setBody("{\"error\": \"" + e.getMessage() + "\"}");
            return response;
        }
        catch (Throwable t)
        {
            t.printStackTrace();

            response.setBody("{\"error\": \"unknown problem parsing form data'\"}");
            response.setStatusCode(400);
            return response;
        }

        try
        {
            validateImages(formListener.getImages());
        }
        catch (FormValidationException e)
        {
            response.setStatusCode(400);
            response.setBody("{\"error\": \"" + e.getMessage() + "\"}");
            return response;
        }
        // formParser.reset();

        Ditherer ditherer = new Ditherer();
        BufferedImage resultImage = ditherer.dither(formListener.getImages());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try
        {
            // TODO: when I somehow set the resultImage to null, the try/catch didn't catch (java.lang.IllegalArgumentException: image == null!)
            ImageIO.write(resultImage, "png", outputStream);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        String JSONImage = "{\"resultImage\": \"" + Base64.getEncoder().encodeToString(outputStream.toByteArray()) + "\"}";

        response.setBody(JSONImage);
        response.setStatusCode(200);
        return response;

        // TODO: ✓ package the data to return it
        // ✓ I'd like to use json formatting because the website can then handle the response in a more standard way
        // but the downside is that we need to encode it as base64, which uses more processing time encoding in the back-end and decoding on the front-end
        // instead, it may be possible to return a image/png response for valid requests but a json response for any issues
        // that way, the front-end doesn't need to perform much extra work other than determining the response type and displaying the appropriate image or error message
    }

    // TODO: make a wrapper of sorts for all of the validation functions since we always just return JSON as "error": "[the thrown exception string]" each time there are validation issues
    public static void JSONifyValidation()
    {

    }

    public static String validateHeaders(APIGatewayProxyRequestEvent event, LambdaLogger logger) throws FormValidationException
    {
        if (!event.getIsBase64Encoded())
        {
            throw new FormValidationException("input image/form was not Base64 encoded");
        }
        if (!event.getHeaders().containsKey("content-type"))
        {
            throw new FormValidationException("'content-type' not found");
        }

        String[] contentTypeHolder = splitContentTypeAndBoundary(event.getHeaders().get("content-type"));
        if (!contentTypeHolder[0].equalsIgnoreCase("multipart/form-data"))
        {
            throw new FormValidationException("'content-type' must be 'multipart/form-data'");
        }
        if (contentTypeHolder.length != 2)
        {
            throw new FormValidationException("multipart/form-data must include boundary");
        }
        logger.log("Boundary: " + contentTypeHolder[1]);
        return contentTypeHolder[1];
    }

    public static void validateImages(Map<String, BufferedImage> images) throws FormValidationException
    {
        for (String key : images.keySet())
        {
            switch(key)
            {
                case "inputImage":
                case "kernel":
                case "palette":
                    break;
                default:
                    throw new FormValidationException("form field names did not match expectations (inputImage, kernel, palette)");
            }
        }
    }

    public static String[] splitContentTypeAndBoundary(String contentTypeAndBoundary)
    {
        //"content-type=multipart/form-data; boundary=----geckoformboundary1da05be6da18d993830a9652a2c4c80e"
        String[] splitString = contentTypeAndBoundary.split("; ", 2);
        if (splitString.length == 2)
        {
            try
            {
                splitString[1] = splitString[1].split("=", 2)[1];
            } catch (ArrayIndexOutOfBoundsException e)
            {

            }
        }
        return splitString;
    }
}