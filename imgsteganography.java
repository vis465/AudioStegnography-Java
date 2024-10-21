import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.imageio.ImageIO;

public class imgsteganography {

    // Method to encode audio data into an image
    public static void encode(String imagePath, String outputPath, String audioPath) throws IOException {
        // Read the image file
        BufferedImage image = ImageIO.read(new File(imagePath));
        int width = image.getWidth();
        int height = image.getHeight();

        // Read the audio file into a byte array
        File audioFile = new File(audioPath);
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());

        // Check if the image is large enough to hold the audio data
        if (audioBytes.length * 8 > width * height) {
            throw new IllegalArgumentException("Image is too small to hold the audio data.");
        }

        int byteIndex = 0; // Current byte of the audio data
        int bitIndex = 0;  // Current bit in the byte

        // Loop through each pixel in the image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                // Get the RGB components of the pixel
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // Embed bits into the red channel
                if (byteIndex < audioBytes.length) {
                    int currentByte = audioBytes[byteIndex];
                    int bit = (currentByte >> (7 - bitIndex)) & 0x01;
                    red = (red & 0xFE) | bit; // Modify the LSB of the red channel

                    // Update bit and byte indices
                    bitIndex++;
                    if (bitIndex == 8) {
                        bitIndex = 0;
                        byteIndex++;
                    }
                }

                // Set the modified pixel back into the image
                int newPixel = (red << 16) | (green << 8) | blue;
                image.setRGB(x, y, newPixel);

                // If all audio data is embedded, stop the process
                if (byteIndex >= audioBytes.length) {
                    break;
                }
            }
            if (byteIndex >= audioBytes.length) {
                break;
            }
        }

        // Save the modified image
        ImageIO.write(image, "png", new File(outputPath));
        System.out.println("Audio data embedded successfully.");
    }

    // Main method to execute the encoding process
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java imgsteganography <imagePath> <outputPath> <audioPath>");
            System.exit(1);
        }

        String imagePath = args[0];
        String outputPath = args[1];
        String audioPath = args[2];

        try {
            // Call the encode method
            encode(imagePath, outputPath, audioPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
