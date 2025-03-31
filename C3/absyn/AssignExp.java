package absyn;

public class AssignExp extends Exp {
    public VarExp lhs;
    public Exp rhs;

    public AssignExp ( int row, int col, VarExp lhs, Exp rhs ) {
        this.row = row;
        this.col = col;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    /* Special case for boolean isAddr */
    // public AssignExp( int row, int col, VarExp lhs, Exp rhs, boolean isAddr) {  
    // }
    
    public void accept( AbsynVisitor visitor, int level, boolean flag ) {
        visitor.visit( this, level, flag );
    }

    public String VarType() {
        SimpleVar variable = (SimpleVar) lhs.variable;
        return variable.toString();
    }

}
