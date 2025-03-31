package absyn;

public class NilExp extends Exp {
    public int type = 2;

    public NilExp( int row, int col ) {
        this.row = row;
        this.col = col;
    }

    public int getType(){
        return type;
    }

    public void accept( AbsynVisitor visitor, int level, boolean flag ) {
        visitor.visit( this, level, flag );
    }
}
