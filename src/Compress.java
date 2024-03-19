
//25857606
import java.io.IOException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

/**
 * Main
 * 
 * @author Ruben Paul Bosma
 *
 */
public class Compress {

    // Set that holds all acceptedWords
    public static Set<String> acceptedWords = new HashSet<>();

    public static int dimension;
    public static int max;

    public static String gui;
    public static String compressed;
    public static String compMode;

    public static boolean decompMR = false;

    /**
     * Main function where all other functions are called and input validated and
     * received
     * 
     * @param args - Input read from args
     * @throws IOException - for error catching
     */
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub

        // Check for invalid number of arguments
        if (args.length < 4 || args.length > 5) {
            System.err.println("Input Error - Invalid number of arguments");
            System.exit(0);
        }

        gui = args[0];
        String mode = args[1];
        String multiRes = args[2];
        String filePath = args[3];

        char modeChar = mode.charAt(0);
        // Validate mode argument
        if (mode.length() > 1 || !(Character.isDigit(modeChar))) {
            System.err.println("Input Error - Invalid argument type");
            System.exit(0);
        }
        // Validate gui argument
        if ((!gui.equals("0") && !gui.equals("1")) || gui.length() > 1) {
            System.err.println("Input Error - Invalid GUI argument");
            System.exit(0);
        }

        // Validate mode argument
        if (!mode.equals("1") && !mode.equals("2")) {
            System.err.println("Input Error - Invalid mode");
            System.exit(0);
        }

        // Validate multiRes argument
        if (!multiRes.equalsIgnoreCase("t") && !multiRes.equalsIgnoreCase("f")) {
            System.err.println("Input Error - Invalid multi-resolution flag");
            System.exit(0);
        }

        // Validate number of arguments for specific modes
        if ((mode.equals("1") || mode.equals("2")) && args.length != 5
                && multiRes.equalsIgnoreCase("t")) {
            System.err.println("Input Error - Invalid number of arguments");
            System.exit(0);
        }

        // Misc validations
        Operations.validation(multiRes, gui, mode, filePath);

        // Case where multires decompression is called
        if ((multiRes.equals("t") || multiRes.equals("T")) && mode.equals("1")) {
            decompMR = true;
            dimension = Integer.parseInt(args[3]);
            filePath = args[4];
        }
        // Case where multires compression is called
        if ((multiRes.equals("t") || multiRes.equals("T")) && mode.equals("2")) {
            compMode = args[3];
            filePath = args[4];
        }

        String fileExtension = filePath.substring(filePath.lastIndexOf('.') + 1);

        // Case of decompression
        if (mode.equals("1")) {
            try (Scanner scanner = new Scanner(new File(filePath))) {
                Decompression.decompProcessing(filePath, multiRes);
            } catch (FileNotFoundException e) {
                System.err.println("Input Error - Invalid or missing file");
                System.exit(0);
            }
        }
        // Case of compression
        if (mode.equals("2")) {
            try (Scanner scanner = new Scanner(new File(filePath))) {
                compProcessing(filePath, multiRes, compMode);
            } catch (FileNotFoundException e) {
                System.err.println("Input Error - Invalid or missing file");
                System.exit(0);
            }
        }
    }

    /**
     * Processing for compression and general setup
     * 
     * @param filePath - filepath args input
     * @param multiRes - multires args input
     * @param compMode - compmode args input
     * @throws IOException - error catching
     */
    private static void compProcessing(String filePath, String multiRes, String compMode)
            throws IOException {
        BufferedImage image = ImageIO.read(new File(filePath));

        int width = image.getWidth();
        int height = image.getHeight();
        if (width != height) {
            System.err.println("Compress Error - Invalid input image");
            System.exit(0);
        }
        // Check if image contains pixel values other than black or white
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                if (rgb != -16777216 && rgb != -1) { // Check for black and white pixel values
                    System.err.println("Compress Error - Invalid input image");
                    System.exit(0);
                }
            }
        }

        // Fill a boolean array with true/false depending on black pixels
        String accepted = "";
        Color black = new Color(0, 0, 0);
        int black1 = black.getRGB();
        Color white = new Color(255, 255, 255);
        int white1 = white.getRGB();
        boolean[][] coloredPixels = new boolean[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (image.getRGB(i, j) == black1) {
                    coloredPixels[i][j] = true;
                }
                if (image.getRGB(i, j) == white1) {
                    coloredPixels[i][j] = false;
                }
            }

        }

        // Multires being called for different compressions
        if (multiRes.equals("t") || multiRes.equals("T")) {
            if (compMode.equals("1")) {
                Operations.getAccepted(coloredPixels, accepted);
                compressSerp(acceptedWords, image);
            }
            if (compMode.equals("2")) {
                Operations.getAccepted(coloredPixels, accepted);
                compressCheck(image, acceptedWords);
            }
            if (compMode.equals("3")) {
                Operations.getAccepted(coloredPixels, accepted);
                compressRed(acceptedWords);
            }
        } else {
            // get AcceptedWords from image
            Operations.getAccepted(coloredPixels, accepted);
            compress(acceptedWords);
        }
        HashSet<String> sortedAW = acceptedWords.stream().sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        System.out.println(sortedAW);

        // Convert output to file
        Operations.fileConvert(filePath, image, compressed);

    }

    /**
     * Implementation of compress algorithm
     * 
     * @param acceptedWords - accepted words obtained from function
     */
    public static void compress(Set<String> acceptedWords) {
        int n = 0;
        int i = 0;

        List<String> transitions = new ArrayList<>();
        List<Set<String>> rawAccept = new ArrayList<>();
        rawAccept.add(acceptedWords);

        while (i <= n) {
            for (int a = 0; a < 4; a++) {
                Set<String> lI = new HashSet<>();

                Set<String> rawL = rawAccept.get(i);
                for (String words : rawL) {
                    if (!words.isEmpty() && words.charAt(0) == (char) (a + '0')) {
                        if (words.length() > 1) {
                            lI.add(words.substring(1));
                        } else {
                            lI.add("");
                        }
                    }
                }

                boolean newTransitionCreated = false;
                for (int j = 0; j <= n; j++) {
                    if (rawAccept.get(j).equals(lI)) {
                        transitions.add(i + " " + j + " " + a);
                        newTransitionCreated = true;
                        break;
                    }
                }

                if (!newTransitionCreated && !lI.isEmpty()) {
                    n++;
                    rawAccept.add(lI);
                    transitions.add(i + " " + n + " " + a);
                }
            }

            i++;
        }

        int acceptCount = 0;
        List<Integer> acceptStates = new ArrayList<>();
        for (int j = 0; j < rawAccept.size(); j++) {
            Set<String> set = rawAccept.get(j);
            if (set.contains("")) {
                acceptCount++;
                acceptStates.add(j);
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(n + 1).append("\n");
        for (int i1 = 0; i1 < acceptCount; i1++) {
            if (i1 == acceptCount - 1) {
                stringBuilder.append(acceptStates.get(i1));
            } else {
                stringBuilder.append(acceptStates.get(i1) + " ");
            }
        }
        stringBuilder.append("\n");
        for (String words : transitions) {
            stringBuilder.append(words).append("\n");
        }

        compressed = stringBuilder.toString();
    }

    /**
     * Compression for serpienski
     * 
     * @param acceptedWords - accepted strings
     * @param image         - input image
     */
    public static void compressSerp(Set<String> acceptedWords, BufferedImage image) {
        int n = 0;
        int i = 0;

        List<String> transitions = new ArrayList<>();
        List<Set<String>> rawAccept = new ArrayList<>();
        rawAccept.add(acceptedWords);

        while (i <= n) {
            for (int a = 0; a < 4; a++) {
                Set<String> lI = new HashSet<>();

                Set<String> rawL = rawAccept.get(i);
                for (String words : rawL) {
                    if (!words.isEmpty() && words.charAt(0) == (char) (a + '0')) {
                        if (words.length() > 1) {
                            lI.add(words.substring(1));
                        } else {
                            lI.add("");
                        }
                    }
                }

                boolean newTransitionCreated = false;
                for (int j = 0; j <= n; j++) {
                    if (rawAccept.get(j).equals(lI)) {
                        transitions.add(i + " " + j + " " + a);
                        newTransitionCreated = true;
                        break;
                    }
                }

                if (!newTransitionCreated && !lI.isEmpty()) {
                    n++;
                    rawAccept.add(lI);
                    transitions.add(i + " " + n + " " + a);
                }
            }

            i++;
        }

        // Find lightest quadrant
        int lightestQuadrant = Operations.findLightestQuadrant(image);
        System.out.println(lightestQuadrant);
        // Add self-loops for all quadrants except the lightest one on accept states
        for (int j = 0; j < rawAccept.size(); j++) {
            Set<String> set = rawAccept.get(j);
            if (set.contains("")) {
                for (int a = 0; a < 4; a++) {
                    if (a != lightestQuadrant) {
                        transitions.add(j + " " + j + " " + a);
                    }
                }
            }
        }

        int acceptCount = 0;
        List<Integer> acceptStates = new ArrayList<>();
        for (int j = 0; j < rawAccept.size(); j++) {
            Set<String> set = rawAccept.get(j);
            if (set.contains("")) {
                acceptCount++;
                acceptStates.add(j);
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(n + 1).append("\n");
        for (int i1 = 0; i1 < acceptCount; i1++) {
            if (i1 == acceptCount - 1) {
                stringBuilder.append(acceptStates.get(i1));
            } else {
                stringBuilder.append(acceptStates.get(i1) + " ");
            }
        }
        stringBuilder.append("\n");
        for (String words : transitions) {
            stringBuilder.append(words).append("\n");
        }

        compressed = stringBuilder.toString();
    }

    /**
     * Compression for checkerboard
     * 
     * @param image         - input image
     * @param acceptedWords - set of accepted strings
     */
    public static void compressCheck(BufferedImage image, Set<String> acceptedWords) {
        int n = 0;
        int i = 0;

        List<String> transitions = new ArrayList<>();
        List<Set<String>> rawAccept = new ArrayList<>();
        rawAccept.add(acceptedWords);

        // Add transitions for checkerboard pattern
        transitions.add("0 " + "0 " + "0");
        transitions.add("0 " + "0 " + "1 ");
        transitions.add("0 " + "0 " + "2");
        transitions.add("0 " + "0 " + "3");

        while (i <= n) {
            for (int a = 0; a < 4; a++) {
                Set<String> lI = new HashSet<>();

                Set<String> rawL = rawAccept.get(i);
                for (String words : rawL) {
                    if (!words.isEmpty() && words.charAt(0) == (char) (a + '0')) {
                        if (words.length() > 1) {
                            lI.add(words.substring(1));
                        } else {
                            lI.add("");
                        }
                    }
                }

                boolean newTransitionCreated = false;
                for (int j = 0; j <= n; j++) {
                    if (rawAccept.get(j).equals(lI)) {
                        transitions.add(i + " " + j + " " + a);
                        newTransitionCreated = true;
                        break;
                    }
                }

                if (!newTransitionCreated && !lI.isEmpty()) {
                    n++;
                    rawAccept.add(lI);
                    transitions.add(i + " " + n + " " + a);
                }
            }

            i++;
        }

        int acceptCount = 0;
        List<Integer> acceptStates = new ArrayList<>();
        for (int j = 0; j < rawAccept.size(); j++) {
            Set<String> set = rawAccept.get(j);
            if (set.contains("")) {
                acceptCount++;
                acceptStates.add(j);
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(n + 1).append("\n");
        for (int i1 = 0; i1 < acceptCount; i1++) {
            if (i1 == acceptCount - 1) {
                stringBuilder.append(acceptStates.get(i1));
            } else {
                stringBuilder.append(acceptStates.get(i1) + " ");
            }
        }
        stringBuilder.append("\n");
        for (String words : transitions) {
            stringBuilder.append(words).append("\n");
        }

        compressed = stringBuilder.toString();
    }

    /**
     * Compression for reduction
     * 
     * @param acceptedWords - set of accepted strings
     */
    public static void compressRed(Set<String> acceptedWords) {
        int n = 0;
        int i = 0;

        List<String> transitions = new ArrayList<>();
        List<Set<String>> rawAccept = new ArrayList<>();
        rawAccept.add(acceptedWords);

        while (i <= n) {
            for (int a = 0; a < 4; a++) {
                Set<String> lI = new HashSet<>();

                Set<String> rawL = rawAccept.get(i);
                for (String words : rawL) {
                    if (!words.isEmpty() && words.charAt(0) == (char) (a + '0')) {
                        if (words.length() > 1) {
                            lI.add(words.substring(1));
                        } else {
                            lI.add("");
                        }
                    }
                }

                boolean newTransitionCreated = false;
                for (int j = 0; j <= n; j++) {
                    if (rawAccept.get(j).equals(lI)) {
                        transitions.add(i + " " + j + " " + a);
                        newTransitionCreated = true;
                        break;
                    }
                }

                if (!newTransitionCreated && !lI.isEmpty()) {
                    n++;
                    rawAccept.add(lI);
                    transitions.add(i + " " + n + " " + a);
                }
            }

            i++;
        }

        // All states are accept states
        int acceptCount = n + 1;
        List<Integer> acceptStates = new ArrayList<>();
        for (int j = 0; j <= n; j++) {
            acceptStates.add(j);
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(n + 1).append("\n");
        for (int i1 = 0; i1 < acceptCount; i1++) {
            if (i1 == acceptCount - 1) {
                stringBuilder.append(acceptStates.get(i1));
            } else {
                stringBuilder.append(acceptStates.get(i1) + " ");
            }
        }
        stringBuilder.append("\n");
        for (String words : transitions) {
            stringBuilder.append(words).append("\n");
        }

        compressed = stringBuilder.toString();
    }

}
