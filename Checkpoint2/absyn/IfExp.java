package absyn;

public class IfExp extends Exp {
    public Exp test;
    public Exp then;
    public Exp elsee;
    
    public IfExp ( int row, int col, Exp test, Exp then, Exp elsee ) {
        this.row = row;
        this.col = col;
        this.test = test;
        this.then = then;
        this.elsee = elsee;
    }

    public void accept( AbsynVisitor visitor, int level ) {
        visitor.visit( this, level );
    }
}
