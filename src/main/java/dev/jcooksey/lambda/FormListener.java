package dev.jcooksey.lambda;

import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.http.MultiPartCompliance;
import org.eclipse.jetty.io.Content;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FormListener implements MultiPart.Parser.Listener
{
    String failureMessage = "";

    String currentFieldName = null;
    FormFileType currentContentType = FormFileType.NONE;
    ByteArrayOutputStream fileByteBuffer = new ByteArrayOutputStream();

    Map<String, BufferedImage> images = new HashMap<>();
    int maxExpectedImages = 1;

    Map<String, String> formFields = new HashMap<>();

    FormListener(int maxExpectedImages)
    {
        this.maxExpectedImages = maxExpectedImages;
    }

    public Map<String, BufferedImage> getImages()
    {
        return images;
    }

    public Map<String, String> getFormFields() { return formFields; }

    @Override
    public void onPartBegin() throws RuntimeException
    {
        // if the form somehow had more images attached to it than the front-end allowed, then the POST should be rejected
        if (images.size() >= maxExpectedImages)
        {
            throw new RuntimeException("number of attached images for form field exceeded expected count (expected " + maxExpectedImages + ")");
        }
    }

    @Override
    public void onPartHeader(String name, String value) throws RuntimeException
    {
        try
        {
            switch (name.toLowerCase())
            {
                case "content-type":
                    // TODO: add other supported image formats
                    if (value.equalsIgnoreCase("image/png"))
                    {
                        currentContentType = FormFileType.PNG;
                    }
                    else if (value.equalsIgnoreCase("image/jpeg"))
                    {
                        currentContentType = FormFileType.JPEG;
                    }
                    else
                    {
                        currentContentType = FormFileType.NONE;
                    }
                    break;
                case "content-disposition":
                    String[] fileMetadata = value.split("; ");
                    for (String attribute : fileMetadata)
                    {
                        String[] attributeSplit = attribute.split("=", 2);
                        String attributeName = attributeSplit[0];
                        if (attributeName.equalsIgnoreCase("name"))
                        {
                            String attributeValue = attributeSplit[1];
                            currentFieldName = attributeValue.replace("\"", "");
                            break;
                        }
                    }
                    break;
            }
        }
        catch (Exception e)
        {
            // TODO: fix that this exception is never propagated to DitherRequestHandler, possibly by saving the exception, stopping the parser/listener, and manually checking for exceptions after parsing is completed or stopped
            throw new RuntimeException("an error occurred whilst parsing form headers");
        }
    }

    @Override
    public void onPartContent(Content.Chunk chunk)
    {
        if (currentContentType == FormFileType.PNG || currentContentType == FormFileType.JPEG || currentContentType == FormFileType.NONE)
        {
            // to later load images into individual BufferedImages, the chunks are converted into bytebuffers which are accumulated into a single buffer
            ByteBuffer byteBuffer = chunk.getByteBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            try
            {
                fileByteBuffer.write(bytes);
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onPartEnd()
    {
        switch (currentContentType)
        {
            case FormFileType.PNG:
                // intentional fall-through so that all supported formats are parsed the same way
            case FormFileType.JPEG:
                try
                {
                    byte[] byteTest = fileByteBuffer.toByteArray();
                    ByteArrayInputStream baisTest = new ByteArrayInputStream(byteTest);
                    BufferedImage src = ImageIO.read(baisTest);
                    BufferedImage inputImage = new BufferedImage(
                            src.getWidth(),
                            src.getHeight(),
                            BufferedImage.TYPE_INT_RGB
                    );
                    Graphics2D g = inputImage.createGraphics();
                    g.drawImage(src, 0, 0, null);
                    g.dispose();
                    images.put(currentFieldName, inputImage);
                } catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                break;
            case FormFileType.NONE:
                // doesn't really matter if the chunk is some garbled string because we can just use a default value if the setting doesn't exist
                String fieldData = fileByteBuffer.toString(StandardCharsets.UTF_8);
                formFields.put(currentFieldName, fieldData);
                break;
        }

        // reset everything for next form part
        currentFieldName = null;
        currentContentType = FormFileType.NONE;
        fileByteBuffer.reset();
    }

    @Override
    public void onFailure(Throwable failure)
    {
        System.err.println("Multipart parsing failed: " + failure.getMessage());
        failureMessage = failure.getMessage();
    }

    /*@Override
    public void onViolation(MultiPartCompliance.Violation violation)
    {
        System.err.println("Multipart parsing violation: " + violation.getName());
        System.err.println("Multipart parsing violation details: " + violation.getDescription());
        failureMessage = violation.getName();
    }*/
}
