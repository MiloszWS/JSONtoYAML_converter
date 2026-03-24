import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Ścieżka do pliku, który chcemy sprawdzić.
        String filePath = "D:\\Studia 2\\MiASI_project\\JSONtoYAML\\correct.json";
        //String filePath = "D:\\Studia 2\\MiASI_project\\JSONtoYAML\\mistakes.json";

        try {
            // 1. Wczytanie strumienia znaków Z PLIKU (kluczowa zmiana!)
            CharStream input = CharStreams.fromFileName(filePath);

            // 2. Tworzenie Lexera
            JSONLexer lexer = new JSONLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowingErrorListener());

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // 3. Tworzenie Parser
            JSONParser parser = new JSONParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowingErrorListener());

            // 4. Rozpoczęcie weryfikacji składni od głównej reguły 'json'
            JSONParser.JsonContext tree = parser.json();

            System.out.println("File '" + filePath + "' is correct");
            System.out.println("Syntax tree: " + tree.toStringTree(parser));

        } catch (IOException e) {
            // Ten błąd wyskoczy, jeśli Java nie znajdzie pliku podaną ścieżką
            System.err.println("FileError: Please check is there a file from Your path location." + e.getMessage());
        } catch (ParseCancellationException e) {
            // Ten błąd wyskoczy, jeśli gramatyka uzna JSON za niepoprawny
            System.err.println("There is syntax error in file: '" + filePath + "'!");
            System.err.println(e.getMessage());
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