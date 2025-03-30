package absyn;

public class SimpleDec extends VarDec {
    public NameTy typ;
    public String name;

    public SimpleDec ( int row, int col, NameTy typ, String name ) {
        this.row = row;
        this.col = col;
        this.typ = typ;
        this.name = name;
    }

    public String toString(){
        if(typ.typ == 0) return "bool";
        if(typ.typ == 1) return "int";
        if(typ.typ == 2) return "void";
        return null;
    }

    public int getType(){
        return typ.typ;
    }

    public void accept( AbsynVisitor visitor, int level ) {
        visitor.visit( this, level );
    }
}
