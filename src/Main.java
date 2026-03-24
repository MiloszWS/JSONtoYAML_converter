import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        // Ścieżka do pliku, który chcemy sprawdzić.
        String inputFilePath = "correct.json";
        String outputFilePath = inputFilePath.replace(".json", ".yaml");
        //String filePath = "D:\\Studia 2\\MiASI_project\\JSONtoYAML\\mistakes.json";

        try {
            // 1. Wczytanie strumienia znaków Z PLIKU (kluczowa zmiana!)
            CharStream input = CharStreams.fromFileName(inputFilePath);

            // 2. Tworzenie Lexera
            JSONLexer lexer = new JSONLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowingErrorListener());

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // 3. Tworzenie Parsera
            JSONParser parser = new JSONParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowingErrorListener());

            // 4. Rozpoczęcie weryfikacji składni od głównej reguły 'json'
            JSONParser.JsonContext tree = parser.json();

            System.out.println("File '" + inputFilePath + "' is correct. Starting conversion");
            System.out.println("Syntax tree: " + tree.toStringTree(parser));

            // 4. Uruchomienie Visitora i transformacja do YAML
            YamlConverterVisitor visitor = new YamlConverterVisitor();
            String yamlOutput = visitor.visit(tree);

            // 5. Zapis wyniku do pliku .yaml
            Path outputPath = Paths.get(outputFilePath);
            Files.writeString(outputPath, yamlOutput);

            System.out.println("Success! Converted filed was saved as: " + outputFilePath);

        } catch (IOException e) {
            // Ten błąd wyskoczy, jeśli Java nie znajdzie pliku podaną ścieżką
            System.err.println("FileError: Please check is there a file from Your path location." + e.getMessage());
        } catch (ParseCancellationException e) {
            // Ten błąd wyskoczy, jeśli gramatyka uzna JSON za niepoprawny
            System.err.println("There is syntax error in file: '" + inputFilePath + "'!");
            System.err.println(e.getMessage());
            System.err.println("Conversion stopped.");
        }
    }
}

// Ta klasa zostaje bez zmian - wciąż świetnie wyłapuje błędy i zatrzymuje program
class ThrowingErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e)
            throws ParseCancellationException {
        String errorMsg = String.format("Error in line %d, column %d: %s", line, charPositionInLine, msg);
        throw new ParseCancellationException(errorMsg);
    }
}