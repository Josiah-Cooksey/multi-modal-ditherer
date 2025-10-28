package dev.jcooksey.executable;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.FileSystems;

public class Main
{

    public static void main(String[] args)
    {
        final String CWD = System.getProperty("user.dir");
        final String SEP = File.separator;

        // for this executable the simplest method is to have an input and output folder
        // however, I may change this later for better UX
        File inputFolder = new File("input");
        File outputFolder = new File("output");
        inputFolder.mkdir();
        outputFolder.mkdir();
        // we check if the folders exist because if they couldn't be created AND they don't exist, then there no way to continue the next steps
        if (!inputFolder.exists())
        {
            System.err.println("Input folder could not be created.");
            return;
        }
        if (!outputFolder.exists())
        {
            System.err.println("Input folder could not be created.");
            return;
        }

        File[] inputFiles = inputFolder.listFiles();
        if (inputFiles == null || inputFiles.length == 0)
        {
            System.err.println("No files exist in input folder.");
            return;
        }

        for (int i = 0; i < inputFiles.length; i++)
        {
            // for now, I'm only supporting PNG and JPEG images
            if (!inputFiles[i].isFile() || !(inputFiles[i].getName().endsWith(".jpeg") || inputFiles[i].getName().endsWith(".jpg") || inputFiles[i].getName().endsWith(".png")))
            {
                continue;
            }

            try
            {
                BufferedImage inputImage = ImageIO.read(inputFiles[i]);
                Graphics g = inputImage.createGraphics();
                g.setColor(new Color(0, 255, 0, 100));
                g.fillRect(0, 0, inputImage.getWidth(), inputImage.getHeight());
                g.dispose();

                File outputFile = new File(outputFolder + SEP + "output.png");
                ImageIO.write(inputImage, "png", outputFile);

            } catch (Exception e)
            {
                System.err.println("Error reading image file: " + inputFiles[i].getAbsolutePath());
                e.printStackTrace();
            }
        }

    }
}
