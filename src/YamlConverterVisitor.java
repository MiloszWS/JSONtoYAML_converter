public class YamlConverterVisitor extends JSONBaseVisitor<String> {

    private int indentLevel = -1; //tracking the current indentation level.

    //spaces generated based on the current indentation level. Each level corresponds to 2 spaces.
    private String getIndent() {
        if (indentLevel <= 0) return "";
        return "  ".repeat(indentLevel);
    }

    //removing any whitespace at the beginning/end
    @Override
    public String visitJson(JSONParser.JsonContext ctx) {
        String result = visit(ctx.value());

        if (result.startsWith("\n")) {
            return result.substring(1);
        }
        return result;
    }

    //formatting (new line)
    @Override
    public String visitObj(JSONParser.ObjContext ctx) {

        if (ctx.pair().isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        indentLevel++;

        for (JSONParser.PairContext pairCtx : ctx.pair()) {
            sb.append("\n").append(getIndent()).append(visit(pairCtx));
        }

        indentLevel--;
        return sb.toString();
    }

    @Override
    public String visitPair(JSONParser.PairContext ctx) {
        String key = ctx.STRING().getText();

        if (key.startsWith("\"") && key.endsWith("\"")) {
            key = key.substring(1, key.length() - 1);
        }

        JSONParser.ValueContext valueCtx = ctx.value();
        String value = visit(valueCtx);

        //If the value is a nested object or array, we do not put a space after the colon,
        if ((valueCtx.obj() != null && !valueCtx.obj().pair().isEmpty()) ||
                (valueCtx.arr() != null && !valueCtx.arr().value().isEmpty())) {
            return key + ":" + value;
        } else {
            return key + ": " + value;
        }
    }

    //Array formatting (new line and dash)
    @Override
    public String visitArr(JSONParser.ArrContext ctx) {

        if (ctx.value().isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        indentLevel++;

        for (JSONParser.ValueContext valCtx : ctx.value()) {
            sb.append("\n").append(getIndent()).append("- ").append(visit(valCtx).trim());
        }

        indentLevel--;
        return sb.toString();
    }

    //Checking the data type
    @Override
    public String visitValue(JSONParser.ValueContext ctx) {
        if (ctx.STRING() != null) return ctx.STRING().getText();
        if (ctx.NUMBER() != null) return ctx.NUMBER().getText();
        if (ctx.obj() != null) return visit(ctx.obj());
        if (ctx.arr() != null) return visit(ctx.arr());

        return ctx.getText();
    }
}