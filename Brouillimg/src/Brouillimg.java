import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class Brouillimg {

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: java Brouillimg <image_claire> <clé> <mode> [image_sortie]");
            System.err.println("  mode: scramble ou unscramble");
            System.exit(1);
        }
        String inPath = args[0];
        String mode = args[2];
        String outPath = (args.length >= 4) ? args[3] : "out.png";
        // Masque 0x7FFF pour garantir que la clé ne dépasse pas les 15 bits
        int key = Integer.parseInt(args[1]) & 0x7FFF;

        BufferedImage inputImage = ImageIO.read(new File(inPath));
        if (inputImage == null) {
            throw new IOException("Format d’image non reconnu: " + inPath);
        }

        final int height = inputImage.getHeight();
        final int width = inputImage.getWidth();
        System.out.println("Dimensions de l'image : " + width + "x" + height);

        // Pré‑calcul des lignes en niveaux de gris pour accélérer le calcul du critère
        int[][] inputImageGL = rgb2gl(inputImage);

        int[] perm = generatePermutation(height, key);

        //BufferedImage scrambledImage = scrambleLines(inputImage, perm);
        BufferedImage outputImage;

        if (mode.equalsIgnoreCase("scramble")) {
            outputImage = scrambleLines(inputImage, perm);
            System.out.println("Image brouillée");
        } else if (mode.equalsIgnoreCase("unscramble")) {
            outputImage = unScrambleLines(inputImage, perm);
            System.out.println("Image débrouillée");
        } else {
            System.err.println("Mode invalide. Utilisez 'scramble' ou 'unscramble'");
            System.exit(1);
            return;
        }
        ImageIO.write(outputImage, "png", new File(outPath));
        System.out.println("Image écrite: " + outPath);

        affiche(generatePermutation(7, 300));
    }

    /**
     * Convertit une image RGB en niveaux de gris (GL).
     *
     * @param inputRGB image d'entrée en RGB
     * @return tableau 2D des niveaux de gris (0-255)
     */
    public static int[][] rgb2gl(BufferedImage inputRGB) {
        final int height = inputRGB.getHeight();
        final int width = inputRGB.getWidth();
        int[][] outGL = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = inputRGB.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                // luminance simple (évite float)
                int gray = (r * 299 + g * 587 + b * 114) / 1000;
                outGL[y][x] = gray;
            }
        }
        return outGL;
    }

    /**
     * Génère une permutation des entiers 0..size-1 en fonction d'une clé.
     *
     * @param size taille de la permutation
     * @param key  clé de génération (15 bits)
     * @return tableau de taille 'size' contenant une permutation des entiers 0..size-1
     */
    public static int[] generatePermutation(int size, int key) {
        int[] scrambleTable = new int[size];
        for (int i = 0; i < size; i++) scrambleTable[i] = scrambledId(i, size, key);
        return scrambleTable;
    }

    /**
     * Affiche un tableau
     *
     * @param tab
     */
    public static void affiche(int[] tab) {
        for (int i = 0; i < tab.length; i++) {
            System.out.print(tab[i] + " ");
        }
    }

    /**
     * Mélange les lignes d'une image selon une permutation donnée.
     *
     * @param inputImg image d'entrée
     * @param perm     permutation des lignes (taille = hauteur de l'image)
     * @return image de sortie avec les lignes mélangées
     */
    public static BufferedImage scrambleLines(BufferedImage inputImg, int[] perm) {
        int width = inputImg.getWidth();
        int height = inputImg.getHeight();
        if (perm.length != height) throw new IllegalArgumentException("Taille d'image <> taille permutation");

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < height; i++) {
            int newid = perm[i];
            for (int j = 0; j < width; j++) {
                int rgb = inputImg.getRGB(j, i);
                out.setRGB(j, newid, rgb);
            }
        }
        return out;
    }

    /**
     * Remets l'image en ordre.
     *
     * @param inputImg
     * @param perm
     * @return
     */
    public static BufferedImage unScrambleLines(BufferedImage inputImg, int[] perm) {
        int width = inputImg.getWidth();
        int height = inputImg.getHeight();
        if (perm.length != height) throw new IllegalArgumentException("Taille d'image <> taille permutation");

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < height; i++) {
            int newid = perm[i];
            for (int j = 0; j < width; j++) {
                int rgb = inputImg.getRGB(j, newid);
                out.setRGB(j, i, rgb);

            }
        }
        return out;
    }


    /**
     * Renvoie la position de la ligne id dans l'image brouillée.
     *
     * @param id   indice de la ligne dans l'image claire (0..size-1)
     * @param size nombre total de lignes dans l'image
     * @param key  clé de brouillage (15 bits)
     * @return indice de la ligne dans l'image brouillée (0..size-1)
     */
    public static int scrambledId(int id, int size, int key) {
        int r = key >> 7;
        int s = key % (r << 7);
        return (r + (2 * s + 1) * id) % size;
    }

    /**
     * Compare l'image brouiller et débrouiller
     *
     * @param inputImg
     * @param outImg
     * @return
     */
    public static BufferedImage comparaison(BufferedImage inputImg, BufferedImage outImg) {
        int width = inputImg.getWidth();
        int height = inputImg.getHeight();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int irgb = inputImg.getRGB(j, i);
                int orgb = outImg.getRGB(j, i);
                if (irgb != orgb){

                }
            }
        }
        return out;
    }
}