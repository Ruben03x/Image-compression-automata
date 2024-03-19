import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;

/**
 * Class for operations regarding compression/decompression
 * 
 * @author ruben
 *
 */
public class Operations {

    private static Set<String> acceptedWords = Compress.acceptedWords;

    /**
     * Validation
     * 
     * @param multiRes - multires args input
     * @param gui      - gui args input
     * @param mode     - mode args input
     * @param filePath - filepath args input
     */
    public static void validation(String multiRes, String gui, String mode, String filePath) {

        // Check argument types
        if (gui.length() != 1) {
            System.err.println("Input Error - Invalid argument type");
            System.exit(0);
        }
        if (mode.length() != 1) {
            System.err.println("Input Error - Invalid argument type");
            System.exit(0);
        }
        if (multiRes.length() != 1) {
            System.err.println("Input Error - Invalid argument type");
            System.exit(0);
        }

        // Check argument validity
        if (!(gui.equals("0") || gui.equals("1"))) {
            System.err.println("Input Error - Invalid GUI argument");
            System.exit(0);
        }
        if (!(mode.equals("1") || mode.equals("2"))) {
            System.err.println("Input Error - Invalid mode");
            System.exit(0);
        }
        if (!(multiRes.equals("t") || multiRes.equals("T") || multiRes.equals("f")
                || multiRes.equals("F"))) {
            System.err.println("Input Error - Invalid multi-resolution flag");
            System.exit(0);
        }
        if (multiRes.length() < 0) {
            System.err.println("Decompress Error - Invalid word length");
            System.exit(0);
        }
    }

    /**
     * Function that recursively checks quadrants for black pixels and generates
     * corresponding accepted words
     * 
     * @param black - boolean array of trues and false for black pixels being true
     *              and white being false
     * @param word  - empty string being parsed through to be built into accepted
     */
    public static void getAccepted(boolean[][] black, String word) {

        int dimension = black.length;
        int halfWH = dimension / 2;

        if (dimension < 2) {
            return;
        }

        boolean[][] quadArr = new boolean[halfWH][halfWH];

        // Quadrant 1
        boolean checkBlack = true;
        for (int i = 0; i < halfWH; i++) {
            for (int j = 0; j < halfWH; j++) {
                quadArr[i][j] = black[i][j];
                checkBlack = checkBlack && black[i][j];
            }
        }
        if (checkBlack) {
            acceptedWords.add(word + "1");
        } else {
            getAccepted(quadArr, word + "1");
        }

        // Quadrant 3
        quadArr = new boolean[halfWH][halfWH]; // Reset quadArr
        checkBlack = true; // Reset checkBlack
        for (int i = halfWH; i < dimension; i++) {
            for (int j = 0; j < halfWH; j++) {
                quadArr[i - halfWH][j] = black[i][j];
                checkBlack = checkBlack && black[i][j];
            }
        }
        if (checkBlack) {
            acceptedWords.add(word + "3");
        } else {
            getAccepted(quadArr, word + "3");
        }

        // Quadrant 2
        quadArr = new boolean[halfWH][halfWH]; // Reset quadArr
        checkBlack = true; // Reset checkBlack
        for (int i = halfWH; i < dimension; i++) {
            for (int j = halfWH; j < dimension; j++) {
                quadArr[i - halfWH][j - halfWH] = black[i][j];
                checkBlack = checkBlack && black[i][j];
            }
        }
        if (checkBlack) {
            acceptedWords.add(word + "2");
        } else {
            getAccepted(quadArr, word + "2");
        }

        // Quadrant 0
        quadArr = new boolean[halfWH][halfWH]; // Reset quadArr
        checkBlack = true; // Reset checkBlack
        for (int i = 0; i < halfWH; i++) {
            for (int j = halfWH; j < dimension; j++) {
                quadArr[i][j - halfWH] = black[i][j];
                checkBlack = checkBlack && black[i][j];
            }
        }
        if (checkBlack) {
            acceptedWords.add(word + "0");
        } else {
            getAccepted(quadArr, word + "0");
        }
    }

    /**
     * Function that returns lightest quadrant (0,1,2,3)
     * 
     * @param image - image to find lighest quadrant on
     * @return - lightest quadrant
     */
    public static int findLightestQuadrant(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Initialize counts for each quadrant
        int[] quadrantCounts = new int[4];

        // Count number of white pixels in each quadrant
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb);
                int quadrant = 0;
                if (x < width / 2) {
                    if (y < height / 2) {
                        quadrant = 1;
                    } else {
                        quadrant = 0;
                    }
                } else {
                    if (y < height / 2) {
                        quadrant = 3;
                    } else {
                        quadrant = 2;
                    }
                }
                if (color.equals(Color.WHITE)) {
                    quadrantCounts[quadrant]++;
                }
            }
        }

        // Find index of the quadrant with the highest white count
        int lightestQuadrant = 0;
        int maxCount = quadrantCounts[0];
        for (int i = 1; i < quadrantCounts.length; i++) {
            if (quadrantCounts[i] > maxCount) {
                lightestQuadrant = i;
                maxCount = quadrantCounts[i];
            }
        }

        return lightestQuadrant;
    }

    /**
     * GUI for decompression
     * 
     * @param inputTxt - input text file
     * @param image    - painted image
     */
    private static void decompGUI(String inputTxt, BufferedImage image) {
        JFrame frame = new JFrame("Decompression GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.5);

        JPanel inputPanel = new JPanel(new BorderLayout());
        JLabel inputLabel = new JLabel("Input:");
        inputPanel.add(inputLabel, BorderLayout.NORTH);

        JTextPane inputTextPane = new JTextPane();
        inputTextPane.setText(inputTxt);
        inputTextPane.setEditable(false); // Disable text editing
        JScrollPane inputScrollPane = new JScrollPane(inputTextPane);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        splitPane.setLeftComponent(inputPanel);

        JPanel outputPanel = new JPanel(new BorderLayout());
        JLabel outputLabel = new JLabel("Output:");
        outputPanel.add(outputLabel, BorderLayout.NORTH);

        JLabel outputImage = new JLabel();
        ImageIcon icon = getScaledImageIcon(image, 500, 500); // Adjust width and height as needed
        outputImage.setIcon(icon);
        outputPanel.add(outputImage, BorderLayout.CENTER);

        splitPane.setRightComponent(outputPanel);

        frame.getContentPane().add(splitPane);
        frame.setVisible(true);
    }

    /**
     * GUI for compression
     * 
     * @param inputTxt - input text file
     * @param image    - painted image
     */
    private static void compGUI(String inputTxt, BufferedImage image) {
        JFrame frame = new JFrame("Compression GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.5);

        JPanel inputPanel = new JPanel(new BorderLayout());
        JLabel inputLabel = new JLabel("Output:");
        inputPanel.add(inputLabel, BorderLayout.NORTH);

        JTextPane inputTextPane = new JTextPane();
        inputTextPane.setText(inputTxt);
        inputTextPane.setEditable(false); // Disable text editing
        JScrollPane inputScrollPane = new JScrollPane(inputTextPane);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        splitPane.setLeftComponent(inputPanel);

        JPanel outputPanel = new JPanel(new BorderLayout());
        JLabel outputLabel = new JLabel("Input:");
        outputPanel.add(outputLabel, BorderLayout.NORTH);

        JLabel outputImage = new JLabel();
        ImageIcon icon = getScaledImageIcon(image, 500, 500); // Adjust width and height as needed
        outputImage.setIcon(icon);
        outputPanel.add(outputImage, BorderLayout.CENTER);

        splitPane.setRightComponent(outputPanel);

        frame.getContentPane().add(splitPane);
        frame.setVisible(true);
    }

    /**
     * Scaling image for for icon
     * 
     * @param image  - final image
     * @param width  - image width
     * @param height - image height
     * @return - the scaled image
     */
    private static ImageIcon getScaledImageIcon(BufferedImage image, int width, int height) {
        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    /**
     * Function that converts file to correct format and outputs it to appropriate
     * path
     * 
     * @param compressed - compressed string builder
     * @param filePath   - file path as received from input
     * @param image      - image that contains black pixels
     * @throws IOException - error catching
     */
    public static void fileConvert(String filePath, BufferedImage image, String compressed)
            throws IOException {

        String fileExtension = filePath.substring(filePath.lastIndexOf('.') + 1);

        String outputFileName = " ";
        if (fileExtension.equals("txt")) {
            File inFile = new File(filePath);

            if (Compress.gui.equals("1")) {
                decompGUI(Decompression.inputTxt, image);
            }

            ImageIO.write(image, "png", new File("out/"
                    + filePath.substring(filePath.lastIndexOf('/'), filePath.lastIndexOf('.'))
                    + "_dec.png"));

        }
        if (fileExtension.equals("png")) {
            outputFileName = filePath.substring(12, filePath.lastIndexOf('.')) + "_cmp.txt";
            File inFile = new File(filePath);
            File outFile = new File(
                    "./out/" + outputFileName.substring(0, outputFileName.length()));

            OutputStream out = new FileOutputStream(outFile);
            String write = compressed;

            if (Compress.gui.equals("1")) {
                compGUI(compressed, image);
            }

            out.write(write.getBytes());
            out.close();
        }

    }
}
