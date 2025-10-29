package dev.jcooksey.lambda;

import org.eclipse.jetty.http.MultiPart;
import org.eclipse.jetty.io.Content;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class FormListener implements MultiPart.Parser.Listener
{
    String currentFieldName = null;
    String currentFilename = null;
    FormFileType currentContentType = FormFileType.NONE;
    ByteArrayOutputStream fileByteBuffer = new ByteArrayOutputStream();

    @Override
    public void onPartBegin()
    {
    }

    @Override
    public void onPartHeader(String name, String value)
    {
        if (name.equalsIgnoreCase("content-type") && value.equalsIgnoreCase("image/png"))
        {
            currentContentType = FormFileType.PNG;
            return;
        }
    }

    @Override
    public void onPartHeaders()
    {
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
                try
                {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(fileByteBuffer.toByteArray()));
                    Graphics g = img.createGraphics();
                    g.setColor(new Color(0, 255, 0, 100));
                    g.fillRect(0, 0, img.getWidth(), img.getHeight());
                    g.dispose();

                    File outputFile = new File(File.separator + "tmp" + File.separator + "output.png");
                    ImageIO.write(img, "png", outputFile);
                } catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                break;
            case JPEG:
                break;
        }


        // reset everything for next form part
        currentFieldName = null;
        currentFilename = null;
        currentContentType = FormFileType.NONE;
        fileByteBuffer.reset();
    }
}
