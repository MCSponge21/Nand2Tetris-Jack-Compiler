import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Compiler {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Compiler <.jack file or directory>");
            return;
        }

        String inputPath = args[0];
        File inputFile = new File(inputPath);
        List<File> jackFilesToCompile = new ArrayList<>();

            if (inputFile.isFile() && inputPath.endsWith(".jack")) {
                jackFilesToCompile.add(inputFile);
            } else if (inputFile.isDirectory()) {
                File[] filesInDir = inputFile.listFiles((dir, name) -> name.endsWith(".jack"));
                if (filesInDir != null) {
                    jackFilesToCompile.addAll(Arrays.asList(filesInDir));
                }
            } else {
                System.out.println("Invalid input: must be a .jack file or a directory.");
                return;
            }

            for (File jackFile : jackFilesToCompile) {
                String outputPath = jackFile.getAbsolutePath().replace(".jack", ".xml");
                String vmOutput = jackFile.getAbsolutePath().replace(".jack", ".vm");

                try (PrintWriter writer = new PrintWriter(outputPath); 
                PrintWriter vmWriter = new PrintWriter(vmOutput);) {
                    String fileContent = new String(Files.readAllBytes(Paths.get(jackFile.getAbsolutePath())), StandardCharsets.UTF_8);

                    Tokenizer.initialize(fileContent);
                    Parser parser = new Parser(writer, vmWriter);

                    parser.compileClass();

                    System.out.println("Successfully compiled " + jackFile.getName() + " to " + outputPath);

                } catch (Exception e) {
                    System.err.println("Error during compilation of " + jackFile.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
    }
}