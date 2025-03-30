package absyn;

public class SimpleVar extends Var {
    public String name;
    public int type = 3;

    public SimpleVar ( int row, int col, String name ) {
        this.row = row;
        this.col = col;
        this.name = name;
    }

    public int getType(){
        return type;
    }

    public String toString(){
        return name;
    }

    public void accept( AbsynVisitor visitor, int level ) {
        visitor.visit( this, level );
    }
}
