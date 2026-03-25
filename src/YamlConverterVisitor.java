public class YamlConverterVisitor extends JSONBaseVisitor<String> {

    // Zmienna do śledzenia aktualnego poziomu wcięć (niezbędne w YAML)
    // ZMIANA 1: Zaczynamy od -1, aby root obiektu miał 0 spacji wcięcia
    private int indentLevel = -1;

    // Metoda pomocnicza do generowania spacji
    private String getIndent() {
        // Zabezpieczenie, aby nie generować ujemnej liczby spacji
        if (indentLevel <= 0) return "";
        return "  ".repeat(indentLevel);
    }
    @Override
    public String visitJson(JSONParser.JsonContext ctx) {
        String result = visit(ctx.value());

        // Punkt startowy - odwiedzamy główną wartość i usuwamy ewentualne białe znaki na początku/końcu
        // ZMIANA 2: Zamiast .trim() usuwamy TYLKO pierwszy znak nowej linii.
        // Dzięki temu nie niszczymy struktury wcięć pierwszej linijki!
        if (result.startsWith("\n")) {
            return result.substring(1);
        }
        return result;
    }

    @Override
    public String visitObj(JSONParser.ObjContext ctx) {
        // Obsługa pustego obiektu
        if (ctx.pair().isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        indentLevel++; // Wchodzimy głębiej - zwiększamy wcięcie

        for (JSONParser.PairContext pairCtx : ctx.pair()) {
            sb.append("\n").append(getIndent()).append(visit(pairCtx));
        }

        indentLevel--; // Wychodzimy - zmniejszamy wcięcie
        return sb.toString();
    }

    @Override
    public String visitPair(JSONParser.PairContext ctx) {
        // Pobieramy klucz (STRING z JSON-a) i usuwamy cudzysłowy dla ładniejszego YAML-a
        String key = ctx.STRING().getText();
        if (key.startsWith("\"") && key.endsWith("\"")) {
            key = key.substring(1, key.length() - 1);
        }

        // Odwiedzamy wartość przypisaną do klucza
        JSONParser.ValueContext valueCtx = ctx.value();
        String value = visit(valueCtx);

        // Jeśli wartością jest zagnieżdżony obiekt lub tablica, nie dajemy spacji po dwukropku,
        // bo formatowanie (nowa linia) robi się w visitObj / visitArr
        if ((valueCtx.obj() != null && !valueCtx.obj().pair().isEmpty()) ||
                (valueCtx.arr() != null && !valueCtx.arr().value().isEmpty())) {
            return key + ":" + value;
        } else {
            // Dla prostych wartości (string, liczba, boolean) dajemy spację
            return key + ": " + value;
        }
    }

    @Override
    public String visitArr(JSONParser.ArrContext ctx) {
        // Obsługa pustej tablicy
        if (ctx.value().isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        indentLevel++; // Zwiększamy wcięcie dla elementów tablicy

        for (JSONParser.ValueContext valCtx : ctx.value()) {
            // W YAML elementy tablicy zaczynają się od myślnika
            sb.append("\n").append(getIndent()).append("- ").append(visit(valCtx).trim());
        }

        indentLevel--;
        return sb.toString();
    }

    @Override
    public String visitValue(JSONParser.ValueContext ctx) {
        // Sprawdzamy, jakim typem danych jest wartość i odpowiednio reagujemy
        if (ctx.STRING() != null) return ctx.STRING().getText();
        if (ctx.NUMBER() != null) return ctx.NUMBER().getText();
        if (ctx.obj() != null) return visit(ctx.obj());
        if (ctx.arr() != null) return visit(ctx.arr());

        // Zwraca 'true', 'false' lub 'null'
        return ctx.getText();
    }
}