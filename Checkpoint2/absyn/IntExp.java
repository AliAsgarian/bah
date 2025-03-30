package absyn;

public class IntExp extends Exp {
    public int value;
    public int type = 1;

    public IntExp ( int row, int col, int value ) {
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