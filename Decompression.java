import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

/**
 * Class for decompressing
 * 
 * @author Ruben Paul Bosma
 *
 */
public class Decompression {

    /**
     * Object for storing transitions
     * 
     * @author Ruben Paul Bosma
     *
     */
    static class Transition {
        int originState;
        int finalState;
        char symbol;

        /**
         * Transition builder
         * 
         * @param originState - origin state
         * @param finalState  - final state
         * @param symbol      - symbol
         */
        Transition(int originState, int finalState, char symbol) {
            this.originState = originState;
            this.finalState = finalState;
            this.symbol = symbol;
        }
    }

    private static Set<String> acceptedWords = Compress.acceptedWords;
    private static int dimension = Compress.dimension;
    private static int max = Compress.max;

    public static String inputTxt;

    /**
     * Basic processing for decomp and setup
     * 
     * @param filePath - filepath args input
     * @param multiRes - multires args input
     * @throws IOException - catch error for no file
     */
    public static void decompProcessing(String filePath, String multiRes) throws IOException {
        // Read in automaton description
        try (Scanner scanner = new Scanner(new File(filePath))) {
            try {
                int numStates = scanner.nextInt();
            } catch (InputMismatchException e) {
                System.err.println("Decompress Error - Invalid automaton formatting");
                System.exit(0);
            }
            List<Integer> acceptStates = new ArrayList<>();
            try {
                scanner.nextLine();
                for (String s : scanner.nextLine().split(" ")) {
                    acceptStates.add(Integer.parseInt(s));
                }
            } catch (InputMismatchException e) {
                System.err.println("Decompress Error - Invalid automaton formatting");
                System.exit(0);
            }

            for (Integer state : acceptStates) {
                if (state < 0) {
                    System.err.println("Decompress Error - Invalid accept state");
                    System.exit(0);
                }
            }

            int maxOriginState = 0;
            int maxFinalState = 0;
            HashSet<Transition> transition = new HashSet<>();
            try {
                while (scanner.hasNext()) {
                    int originState = scanner.nextInt();
                    int finalState = scanner.nextInt();
                    char symbol = scanner.next().charAt(0);

                    // Check state numbers
                    if (originState < 0 || finalState < 0) {
                        System.err.println("Decompress Error - Invalid transition");
                        System.exit(0);
                    }

                    // Check alphabet character
                    if (Character.isLetter(symbol)) {
                        System.err.println("Decompress Error - Invalid transition");
                        System.exit(0);
                    }

                    maxOriginState = Math.max(maxOriginState, originState);
                    maxFinalState = Math.max(maxFinalState, finalState);
                    transition.add(new Transition(originState, finalState, symbol));
                }

            } catch (InputMismatchException e) {
                System.err.println("Decompress Error - Invalid automaton formatting");
                System.exit(0);
            }

            if (Compress.gui.equals("1")) {
                inputTxt = "";
                try {
                    Scanner myReader = new Scanner(new File(filePath));
                    while (myReader.hasNextLine()) {
                        inputTxt += myReader.nextLine() + "\n";
                    }
                    myReader.close();
                } catch (FileNotFoundException e) {
                    System.err.println("Decompress Error - Invalid automaton formatting");
                    System.exit(0);
                }
            }

            // Convert the HashSet to a List
            List<Transition> transitions = new ArrayList<>(transition);
            StringBuilder word = new StringBuilder();

            long startTA = System.nanoTime();

            if (!Compress.decompMR) {
                System.out.println("Normal FA" + "\n");
                // Traverse automaton, starting at node 0, and finally filling acceptedWords set
                traverseAutomaton(0, word, transitions, acceptedWords, acceptStates, maxFinalState);
            } else {
                System.out.println("reduction FA" + "\n");
                traverseAutomatonMR(0, word, transitions, acceptedWords, acceptStates,
                        maxFinalState);
            }

            long endTA = System.nanoTime();
            long elapsedTimeTA = (endTA - startTA) / 1_000_000;
            System.out.println("Traverse automaton: " + elapsedTimeTA + "ms");

            acceptedWords.remove("");
            // Get longestString in acceptedWords set and apply it to dimension variable
            long startLS = System.nanoTime();
            String longestString = "";
            Optional<String> longestStringOpt = acceptedWords.stream()
                    .max(Comparator.comparingInt(String::length));
            longestString = longestStringOpt.orElse("");
            long endLS = System.nanoTime();
            long elapsedTimeLS = (endLS - startLS) / 1_000_000;
            System.out.println("Longest string:     " + elapsedTimeLS + "ms");

            if (!(multiRes.equals("t") || multiRes.equals("T"))) {
                dimension = longestString.length();
            }

            // Calculation of width,height and max
            int width = (int) Math.pow(2, dimension);
            int height = (int) Math.pow(2, dimension);
            max = (int) Math.pow(2, dimension);

            // Create blank white canvas
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            // Run acceptedWords through getQuadrantCoordinates that translates the words to
            // coordinates and paints the coordinates black on the image
            processAndPaint(image, g2d);

            // Convert image to correct format
            Operations.fileConvert(filePath, image, "");
        } catch (FileNotFoundException e) {
            System.err.println("Input Error - Invalid or missing file");
            System.exit(0);
        }
    }

    /**
     * Obtains accepted words and alters them as necessary
     * 
     * @param image - image to be painted on
     * @param g2d   - graphics2D support
     */
    private static void processAndPaint(BufferedImage image, Graphics2D g2d) {

        // Create set for acceptedWords smaller than longestString
        Set<String> smallerAW = new HashSet<>();

        long startGS = System.nanoTime();
        for (String word1 : acceptedWords) {
            if (word1.length() < dimension) {
                smallerAW = generateSequences(word1, dimension);
                for (String newWord : smallerAW) {
                    getQuadrantCoordinates(newWord, max, image, g2d, dimension);
                }
            } else {
                getQuadrantCoordinates(word1, max, image, g2d, dimension);
            }
        }
        long endGS = System.nanoTime();
        long elapsedTimeGS = (endGS - startGS) / 1_000_000;
        System.out.println("Sequence:           " + elapsedTimeGS + "ms");
    }

    /**
     * Function used to traverse the automaton and produce the accepted words and
     * add them to the <String> Set, uses a recursion to ensure all accepted words
     * will be added to set
     * 
     * @param state         - starts at 0 then tracks current state
     * @param word          - words that are not yet confirmed whether accepted
     * @param transitions   - set of all transitions
     * @param acceptedWords - acceptedWords set where recognized words will be added
     *                      to
     * @param acceptStates  - accept states for automaton
     * @param maxFinalState - maximum final state
     */
    private static void traverseAutomaton(int state, StringBuilder word,
            List<Transition> transitions, Set<String> acceptedWords, List<Integer> acceptStates,
            int maxFinalState) {
        if (acceptStates.contains(state) && !acceptedWords.contains(word.toString())) {
            acceptedWords.add(word.toString());
        }

        for (Transition transition : transitions) {
            if (transition.originState == state && transition.finalState <= maxFinalState) {
                word.append(transition.symbol);
                traverseAutomaton(transition.finalState, word, transitions, acceptedWords,
                        acceptStates, maxFinalState);
                word.deleteCharAt(word.length() - 1);
            }
        }
    }

    /**
     * Function used to traverse the automaton and produce the accepted words and
     * add them to the <String> Set, uses a recursion to ensure all accepted words
     * will be added to set
     * 
     * @param state         - starts at 0 then tracks current state
     * @param word          - words that are not yet confirmed whether accepted
     * @param transitions   - set of all transitions
     * @param acceptedWords - acceptedWords set where recognized words will be added
     *                      to
     * @param acceptStates  - accept states for automaton
     * @param maxFinalState - maximum final state
     */
    private static void traverseAutomatonMR(int state, StringBuilder word,
            List<Transition> transitions, Set<String> acceptedWords, List<Integer> acceptStates,
            int maxFinalState) {
        if (acceptStates.contains(state) && !acceptedWords.contains(word)
                && (word.length() == dimension || dimension == 0)) {
            acceptedWords.add(word.toString());
        }

        if (dimension == 0 || word.length() < dimension) {
            for (Transition transition : transitions) {
                if (transition.originState == state && transition.finalState <= maxFinalState) {
                    word.append(transition.symbol);
                    traverseAutomatonMR(transition.finalState, word, transitions, acceptedWords,
                            acceptStates, maxFinalState);
                    word.deleteCharAt(word.length() - 1);
                }
            }
        }
    }

    /**
     * Function that fills the rest of shorter strings with all the possible
     * sequences of numbers in order for it to be applicable and larger for size
     * appropriate grid
     * 
     * @param word   - word that will be appended to
     * @param length - predetermined length thats the goal
     * @return - Return sequences which is the words adapted to length in a set
     */
    public static Set<String> generateSequences(String word, int length) {
        Set<String> sequences = new HashSet<>();
        generateSequencesHelper(word, length, sequences);
        return sequences;
    }

    /**
     * Function that fills the rest of shorter strings with all the possible
     * sequences of numbers in order for it to be applicable and larger for size
     * appropriate grid, this is the helper function
     * 
     * @param currentWord - tracks current state of word
     * @param length      - predetermined length
     * @param sequences   - where words are added to
     */
    private static void generateSequencesHelper(String currentWord, int length,
            Set<String> sequences) {
        if (currentWord.length() == length) {
            sequences.add(currentWord);
        } else {
            for (int i = 0; i < 4; i++) {
                generateSequencesHelper(currentWord + i, length, sequences);
            }
        }
    }

    /**
     * Function used to get the coordinated corresponding to image and paints black
     * pixels once x,y coordinates are produced
     * 
     * @param word - accepted word, will be every accepted word
     * @param max  - represents max quadrants
     * @param img  - image where pixels are painted on
     * @param g2d  - graphics 2D used for image processing
     * @param quad - represents longestString
     * @return array of x,y coords
     */
    public static int[] getQuadrantCoordinates(String word, int max, BufferedImage img,
            Graphics2D g2d, int quad) {
        int y = 0;
        int x = 0;

        int xMult = 2;
        int yMult = 2;

        // If the word length equals max string, the operations are applied to the
        // accepted word
        // and coordinates are produced
        if (word.length() == quad) {
            for (int i = 0; i < word.length(); i++) {
                int digit = Character.getNumericValue(word.charAt(i));

                switch (digit) {
                case 0:
                    y += max / yMult;
                    break;
                case 2:
                    x += max / xMult;
                    y += max / yMult;
                    break;
                case 3:
                    x += max / xMult;
                    break;
                default:
                    break;
                }

                // x multiplier and y multiplier scales appropriately for quadrants
                xMult *= 2;
                yMult *= 2;
            }
            // Paint pixel black given x,y
            Color black = new Color(0, 0, 0);
            int black1 = black.getRGB();
            img.setRGB(x, y, black1);
        }
        return new int[] {x, y};

    }

}
