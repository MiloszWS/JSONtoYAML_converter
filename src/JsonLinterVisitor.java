import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonLinterVisitor extends JSONBaseVisitor<Void> {

    //List containing all the warnings
    private final List<String> warnings = new ArrayList<>();
    //List containing critical errors that block conversion
    private final List<String> criticalErrors = new ArrayList<>();

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getCriticalErrors() {
        return criticalErrors;
    }

    @Override
    public Void visitArr(JSONParser.ArrContext ctx) {
        int totalElements = ctx.value().size();
        int nullCount = 0;

        //Looking for null elements in the array
        for (JSONParser.ValueContext val : ctx.value()) {
            if (val.getText().equals("null")) {
                nullCount++;
            }
        }

        if (totalElements >= 5 && nullCount == 1) {
            warnings.add("Suspicious array: contains " + totalElements + " elements, but exactly 1 is 'null'.");
        }

        for (JSONParser.ValueContext val : ctx.value()) {
            if (val.obj() != null && val.obj().pair().isEmpty()) {
                warnings.add("Suspicious array element: empty object {} found inside the array.");
                break;
            }
        }
        return super.visitArr(ctx);
    }

    @Override
    public Void visitObj(JSONParser.ObjContext ctx) {
        Set<String> keys = new HashSet<>();

        //Looking for duplicated elements in json file (CRITICAL ERROR)
        for (JSONParser.PairContext pair : ctx.pair()) {
            String key = pair.STRING().getText();
            if (!keys.add(key)) {
                int line = pair.getStart().getLine();
                criticalErrors.add("Line " + line + ": Duplicate key found: " + key + ". This is strictly forbidden in YAML mapping.");
            }
        }
        return super.visitObj(ctx);
    }
}