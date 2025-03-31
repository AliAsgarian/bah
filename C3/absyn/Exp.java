package absyn;

abstract public class Exp extends Absyn {
    public int dtype;
    
    public int getType(){
        return -1;
    }
}
