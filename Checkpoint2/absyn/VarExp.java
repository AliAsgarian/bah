package absyn;

public class VarExp extends Exp {
    public Var variable;

    public VarExp ( int row, int col, Var variable ) {
        this.row = row;
        this.col = col;
        this.variable = variable;
    }

    public int getType(){
        return variable.getType();
    }

    public String toString(){
        return variable.toString();
    }

    public void accept( AbsynVisitor visitor, int level ) {
        visitor.visit( this, level );
    }
}
