package dev.jcooksey.executable;

import dev.jcooksey.core.Ditherer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

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


        ArrayList<Color> paletteColors = new ArrayList<>();
        paletteColors.add(Color.BLACK);
        paletteColors.add(Color.WHITE);

        paletteColors.add(getRandomColor());
        paletteColors.add(getRandomColor());
        /*paletteColors.add(getRandomColor());
        paletteColors.add(getRandomColor());
        paletteColors.add(getRandomColor());
        paletteColors.add(getRandomColor());
        paletteColors.add(getRandomColor());
        paletteColors.add(getRandomColor());
        paletteColors.add(getRandomColor());*/
        /*paletteColors.add(Color.CYAN);
        paletteColors.add(Color.MAGENTA);
        paletteColors.add(Color.YELLOW);
        paletteColors.add(Color.RED);
        paletteColors.add(Color.GREEN);
        paletteColors.add(Color.BLUE);*/
        for (int i = 0; i < inputFiles.length; i++)
        {
            // for now, I'm only supporting PNG and JPEG images
            if (!inputFiles[i].isFile() || !(inputFiles[i].getName().endsWith(".jpeg") || inputFiles[i].getName().endsWith(".jpg") || inputFiles[i].getName().endsWith(".png")))
            {
                continue;
            }

            try
            {
                BufferedImage src = ImageIO.read(inputFiles[i]);
                BufferedImage inputImage = new BufferedImage(
                    src.getWidth(),
                    src.getHeight(),
                    BufferedImage.TYPE_INT_RGB
                );
                Graphics2D g = inputImage.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, inputImage.getWidth(), inputImage.getHeight());
                g.drawImage(src, 0, 0, null);
                g.dispose();

                Ditherer ditherer = new Ditherer();
                ditherer.setPalette(paletteColors);
                BufferedImage outputImage = ditherer.simpleDither(inputImage);

                File outputFile = new File(outputFolder + SEP + inputFiles[i].getName() + "-dithered.png");
                ImageIO.write(outputImage, "png", outputFile);

            } catch (Exception e)
            {
                System.err.println("Error reading image file: " + inputFiles[i].getAbsolutePath());
                e.printStackTrace();
            }
        }

    }

    public static Color getRandomColor()
    {
        return new Color((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));
    }
}
