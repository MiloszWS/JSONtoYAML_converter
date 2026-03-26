import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String inputFilePath = "weirdo.json";
        String outputFilePath = inputFilePath.replace(".json", ".yaml");
        //String filePath = "D:\\Studia 2\\MiASI_project\\JSONtoYAML\\mistakes.json";

        try {
            //Reading CharStream from the input file
            CharStream input = CharStreams.fromFileName(inputFilePath);


            JSONLexer lexer = new JSONLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowingErrorListener());

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            JSONParser parser = new JSONParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowingErrorListener());

            //JSON Syntax verification
            JSONParser.JsonContext tree = parser.json();

            System.out.println("File '" + inputFilePath + "' is correct. Starting conversion");
            System.out.println("Syntax tree: " + tree.toStringTree(parser));

            //Semantic analysis
            JsonLinterVisitor linter = new JsonLinterVisitor();
            linter.visit(tree);

            //CHECK FOR CRITICAL ERRORS (FATAL)
            List<String> criticalErrors = linter.getCriticalErrors();
            if (!criticalErrors.isEmpty()) {
                System.err.println("\nFATAL SEMANTIC ERRORS DETECTED:");
                for (String err : criticalErrors) {
                    System.err.println(" - " + err);
                }
                System.err.println("\nConversion aborted. YAML specification does not allow these structures.");
                return; // Hard stop - end program
            }

            //CHECK FOR WARNINGS (USER DECISION)
            List<String> warnings = linter.getWarnings();

            if (!warnings.isEmpty()) {
                System.out.println("\nWARNINGS DETECTED DURING SEMANTIC ANALYSIS:");
                for (String w : warnings) {
                    System.out.println(" - " + w);
                }

                System.out.print("\nDo you want to proceed with YAML conversion anyway? (Y/N): ");
                Scanner scanner = new Scanner(System.in);
                String answer = scanner.nextLine().trim().toUpperCase();

                if (!answer.equals("Y")) {
                    System.out.println("Conversion aborted by user.");
                    return;
                }
                System.out.println("Proceeding with conversion...\n");
            }

            //Transformation into .yaml format
            YamlConverterVisitor visitor = new YamlConverterVisitor();
            String yamlOutput = visitor.visit(tree);

            Path outputPath = Paths.get(outputFilePath);
            Files.writeString(outputPath, yamlOutput);

            System.out.println("Success! Converted filed was saved as: " + outputFilePath);

        } catch (IOException e) {
            System.err.println("FileError: Please check is there a file from Your path location." + e.getMessage());
        } catch (ParseCancellationException e) {
            System.err.println("There is syntax error in file: '" + inputFilePath + "'!");
            System.err.println(e.getMessage());
            System.err.println("Conversion stopped.");
        }
    }
}

class ThrowingErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e)
            throws ParseCancellationException {
        String errorMsg = String.format("Error in line %d, column %d: %s", line, charPositionInLine, msg);
        throw new ParseCancellationException(errorMsg);
    }
}