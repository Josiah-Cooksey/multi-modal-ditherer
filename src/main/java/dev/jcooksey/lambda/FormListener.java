package dev.jcooksey.lambda;

import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.io.Content;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FormListener implements MultiPart.Parser.Listener
{
    String currentFieldName = null;
    FormFileType currentContentType = FormFileType.NONE;
    ByteArrayOutputStream fileByteBuffer = new ByteArrayOutputStream();

    Map<String, BufferedImage> images = new HashMap<String, BufferedImage>();
    int maxExpectedImages = 1;

    FormListener(int maxExpectedImages)
    {
        this.maxExpectedImages = maxExpectedImages;
    }

    public Map<String, BufferedImage> getImages()
    {
        return images;
    }

    @Override
    public void onPartBegin() throws RuntimeException
    {
        // if the form somehow had more images attached to it than the front-end allowed, then the POST should be rejected
        if (images.size() >= maxExpectedImages)
        {
            throw new RuntimeException("number of form fields exceeded expected count (expected " + maxExpectedImages + ")");
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
            // TODO: fix that this exception is never propogated to DitherRequestHandler, possibly by saving the exception, stopping the parser/listener, and manually checking for exceptions after parsing is completed or stopped
            throw new RuntimeException("an error occurred whilst parsing form headers");
        }
    }

    @Override
    public void onPartContent(Content.Chunk chunk)
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

    @Override
    public void onPartEnd()
    {
        switch (currentContentType)
        {
            case PNG:
                // intentional fall-through so that all supported formats are parsed the same way
            case JPEG:
                try
                {
                    images.put(currentFieldName, ImageIO.read(new ByteArrayInputStream(fileByteBuffer.toByteArray())));
                } catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                break;
        }

        // reset everything for next form part
        currentFieldName = null;
        currentContentType = FormFileType.NONE;
        fileByteBuffer.reset();
    }
}
