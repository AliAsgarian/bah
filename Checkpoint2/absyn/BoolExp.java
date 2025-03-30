package absyn;

public class BoolExp extends Exp {
    public boolean value;
    public int type = 0;

    public BoolExp ( int row, int col, boolean value ) {
        this.row = row;
        this.col = col;
        this.value = value;
    }

    public int getType(){
        return type;
    }
    
    public void accept( AbsynVisitor visitor, int level ) {
        visitor.visit( this, level );
    }
}