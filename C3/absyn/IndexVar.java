package absyn;

public class IndexVar extends Var {
    public String name;
    public Exp index;

    public IndexVar ( int row, int col, String name, Exp index ) {
        this.row = row;
        this.col = col;
        this.name = name;
        this.index = index;
    }

    public int getType(){
        return index.getType();
    }
    
    public String toString(){
        return name;
    }

    public void accept( AbsynVisitor visitor, int level, boolean flag ) {
        visitor.visit( this, level, flag );
    }
}
