package absyn;

public class VarDecList extends Absyn {
    public VarDec head;
    public VarDecList tail;
  
    public VarDecList( VarDec head, VarDecList tail ) {
      this.head = head;
      this.tail = tail;
    }

    public String toString() {
      String varTypes = "";
      VarDecList pointer = this;
      for(; pointer != null; pointer = pointer.tail) {
        varTypes += pointer.head;
        if(pointer.tail != null)
          varTypes += ", ";
      }
      return varTypes;
    }
  
    public void accept( AbsynVisitor visitor, int level, boolean flag ) {
      visitor.visit( this, level, flag );
    }
}
