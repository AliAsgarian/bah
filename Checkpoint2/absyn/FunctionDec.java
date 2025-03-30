package absyn;

public class FunctionDec extends Dec {
    public NameTy result;
    public String func;
    public VarDecList params;
    public Exp body;

    public FunctionDec ( int row, int col, NameTy result, String func, VarDecList params, Exp body ) {
        this.row = row;
        this.col = col;
        this.result = result;
        this.func = func;
        this.params = params;
        this.body = body;
    }

    public String toString() {
        String parameters = (params != null) ? "(" + params.toString() + ")" : "(void)";
        if(result.typ == 0) return parameters + " -> bool";
        if(result.typ == 1) return parameters + " -> int";
        if(result.typ == 2) return parameters + " -> void";
        return null;
    }

    public void accept( AbsynVisitor visitor, int level ) {
        visitor.visit( this, level );
    }
}
