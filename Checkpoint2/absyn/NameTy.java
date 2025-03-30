package absyn;

public class NameTy extends Absyn {
    public final static int BOOL = 0;
    public final static int INT = 1;
    public final static int VOID = 2;
    public final static int NULL = 3;

    public int typ;

    public NameTy( int row, int col, int typ ) {
        this.row = row;
        this.col = col;
        this.typ = typ;
    }

    public void accept( AbsynVisitor visitor, int level ) {
        visitor.visit( this, level );
    }
}
