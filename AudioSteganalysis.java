import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class AudioSteganalysis {

    // Method to decode the embedded audio from the image
    public static void decodeAudio(String imagePath, String outputAudioPath) throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));

        int width = image.getWidth();
        int height = image.getHeight();

        ByteArrayOutputStream audioData = new ByteArrayOutputStream();

        int bitIndex = 0;
        int currentByte = 0;

        // Loop through each pixel in the image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                int red = (pixel >> 16) & 0xFF;
                int bit = red & 0x01;

                currentByte = (currentByte << 1) | bit;
                bitIndex++;

                // Once we have a full byte (8 bits)
                if (bitIndex == 8) {
                    audioData.write(currentByte);
                    currentByte = 0;
                    bitIndex = 0;
                }
            }
        }

        // Save the extracted audio to a file
        try (FileOutputStream fos = new FileOutputStream(outputAudioPath)) {
            fos.write(audioData.toByteArray());
        }

        System.out.println("Audio extracted successfully to: " + outputAudioPath);
    }

    // Main method to execute the decoding process
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java AudioSteganalysis <imagePath> <outputAudioPath>");
            System.exit(1);
        }

        String imagePath = args[0];
        String outputAudioPath = args[1];

        try {
            decodeAudio(imagePath, outputAudioPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
