import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Nie udało się załadować systemowego wyglądu.");
        }

        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        // --- 1. GŁÓWNE OKNO ---
        JFrame frame = new JFrame("JSON to YAML Converter & Linter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 650);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(245, 246, 250));

        // --- 2. POLA TEKSTOWE ---
        JTextArea jsonInput = new JTextArea("{\n  \"test\": \"Przeciągnij tu plik .json lub wklej tekst\"\n}");
        jsonInput.setFont(new Font("Consolas", Font.PLAIN, 14));
        jsonInput.setMargin(new Insets(10, 10, 10, 10));

        JTextArea yamlOutput = new JTextArea("Tutaj pojawi się wynik YAML lub błędy...");
        yamlOutput.setEditable(false);
        yamlOutput.setBackground(new Color(40, 44, 52));
        yamlOutput.setForeground(new Color(171, 178, 191));
        yamlOutput.setFont(new Font("Consolas", Font.PLAIN, 14));
        yamlOutput.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollLeft = new JScrollPane(jsonInput);
        scrollLeft.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Wejście (JSON)", TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12)));

        JScrollPane scrollRight = new JScrollPane(yamlOutput);
        scrollRight.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Wyjście (YAML / Logi)", TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12), Color.DARK_GRAY));

        // --- 3. PRZYCISK ---
        JButton processButton = new JButton("Konwertuj do YAML");
        processButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        processButton.setBackground(new Color(0, 123, 255));
        processButton.setForeground(Color.WHITE);
        processButton.setFocusPainted(false);
        processButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        processButton.setPreferredSize(new Dimension(200, 40));

        // Akcja przycisku - przekazujemy też 'frame', by móc wyświetlić na nim Popup
        processButton.addActionListener(e -> {
            String result = processJsonPipeline(jsonInput.getText(), frame);
            yamlOutput.setText(result);
        });

        // --- 4. MAGIA DRAG & DROP ---
        jsonInput.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    if (!droppedFiles.isEmpty()) {
                        File file = droppedFiles.get(0);
                        String content = new String(Files.readAllBytes(file.toPath()));
                        jsonInput.setText(content);

                        // Automatycznie odpalamy konwersję po wrzuceniu pliku!
                        yamlOutput.setText(processJsonPipeline(content, frame));
                    }
                } catch (Exception ex) {
                    yamlOutput.setText("Błąd odczytu pliku:\n" + ex.getMessage());
                }
            }
        });

        // --- 5. UKŁADANIE OKNA ---
        JPanel textPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        textPanel.setOpaque(false);
        textPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        textPanel.add(scrollLeft);
        textPanel.add(scrollRight);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        buttonPanel.add(processButton);

        frame.add(textPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // --- TWOJA ZMODYFIKOWANA LOGIKA (Zastąpiony Scanner) ---
    public static String processJsonPipeline(String jsonText, Component parentFrame) {
        try {
            // 1. Syntax Check (Czytanie ze Stringa zamiast z pliku)
            CharStream input = CharStreams.fromString(jsonText);
            JSONLexer lexer = new JSONLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowingErrorListener());

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            JSONParser parser = new JSONParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowingErrorListener());

            JSONParser.JsonContext tree = parser.json();

            // 2. Semantic Analysis
            JsonLinterVisitor linter = new JsonLinterVisitor();
            linter.visit(tree);

            // CHECK FOR CRITICAL ERRORS (FATAL)
            List<String> criticalErrors = linter.getCriticalErrors();
            if (!criticalErrors.isEmpty()) {
                StringBuilder errBuilder = new StringBuilder("❌ FATAL SEMANTIC ERRORS DETECTED:\n\n");
                for (String err : criticalErrors) {
                    errBuilder.append(" - ").append(err).append("\n");
                }
                errBuilder.append("\nConversion aborted. YAML specification does not allow these structures.");
                return errBuilder.toString(); // Zwracamy błąd do prawego okna
            }

            // CHECK FOR WARNINGS (USER DECISION - ZAMIAST SCANNERA UŻYWAMY POPUPU)
            List<String> warnings = linter.getWarnings();
            if (!warnings.isEmpty()) {
                StringBuilder warnBuilder = new StringBuilder("WARNINGS DETECTED DURING SEMANTIC ANALYSIS:\n\n");
                for (String w : warnings) {
                    warnBuilder.append(" - ").append(w).append("\n");
                }
                warnBuilder.append("\nDo you want to proceed with YAML conversion anyway?");

                // Wyskakujące okienko z zapytaniem (Zastępuje Scanner System.in)
                int userChoice = JOptionPane.showConfirmDialog(
                        parentFrame,
                        warnBuilder.toString(),
                        "Ostrzeżenia analizatora",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                // Jeśli użytkownik kliknie "Nie" lub zamknie okienko
                if (userChoice != JOptionPane.YES_OPTION) {
                    return "⚠️ Conversion aborted by user.";
                }
            }

            // 3. Transformation into .yaml format
            YamlConverterVisitor visitor = new YamlConverterVisitor();
            String yamlOutput = visitor.visit(tree);

            return "✅ Konwersja zakończona sukcesem!\n\n" + yamlOutput;

        } catch (ParseCancellationException e) {
            return "❌ BŁĄD SKŁADNIOWY (SYNTAX ERROR):\n\n" + e.getMessage();
        } catch (Exception e) {
            return "Wystąpił nieoczekiwany błąd:\n" + e.getMessage();
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