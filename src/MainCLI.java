import java.io.IOException;

public class MainCLI {
    static String wordlist = "/home/ben/parrot/rockyou.txt";

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }
        HashQueue hash = new HashQueue();
        try {
            hash.addHash(args[0]);
            hash.attack(wordlist, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(hash);
    }

    private static void printHelp() {
        System.out.println("Half-Hashed Kitty CLI");
        System.out.println("A simple command-line interface for attacking a single hash.");
        System.out.println("\nUsage:");
        System.out.println("\tjava MainCLI <hash>");
    }
}
