import absyn.*;
import java.util.HashMap;
import java.util.Stack;
import java.util.Iterator;
import java.util.ArrayList;

public class SemanticAnalyzer implements AbsynVisitor {

    public HashMap<String, ArrayList<NodeType>> symbolTable;
    public Stack<String> stack;
    public int nest = 0;

    public String[] TYPES = {"BOOL", "INT", "VOID"};

    final static int SPACES = 4;

    public SemanticAnalyzer() {
        symbolTable = new HashMap<String, ArrayList<NodeType>>();
        stack = new Stack<String>();
        stack.add("global");
    }

    private void indent( int level ) {
        for( int i = 0; i < level * SPACES; i++ ) System.out.print( " " );
    }

    public void insert(String key, NodeType node) {
        ArrayList<NodeType> nodeList = symbolTable.get(key);

        if(nodeList == null) {
            nodeList = new ArrayList<NodeType>();
            nodeList.add(node);
            symbolTable.put(stack.peek(), nodeList);
        } else { 
            if(!nodeList.contains(node)) 
                nodeList.add(node);
        }
    }

    public NodeType lookup(String name, int row, int col) {
        NodeType node = nodeExists(name);
        return node;
    }
    
    public NodeType funcExists(String name){
        ArrayList<NodeType> list = symbolTable.get("global");
        if(list != null) {
            for(int i = 0; i < list.size(); i++) {
                if(list.get(i).name.equals(name)) {
                    return list.get(i);
                }
            }
        }
        return null;
    }

    public NodeType nodeExists(String name){
        // If stack is not currently global, then don't check global. Else, check global
        Iterator<String> scope = stack.iterator();

        if (!stack.peek().equals("global"))
            scope.next();

        while( scope.hasNext() ) {
            ArrayList<NodeType> list = symbolTable.get(scope.next());
            if(list != null) {
                for(int i = 0; i < list.size(); i++) {
                    if(list.get(i).name.equals(name)) {
                        return list.get(i);
                    }
                }
            }
        }
        if (!stack.peek().equals("global")){
            ArrayList<NodeType> list = symbolTable.get("global");
            if(list != null) {
                for(int i = 0; i < list.size(); i++) {
                    if(list.get(i).name.equals(name)) {
                        return list.get(i);
                    }
                }
            }
        }
        return null;
    }

    public boolean isDeclared(String name, String type, int row, int col){
        NodeType node = lookup(name, row, col);
        if ((node == null || node.level == 0))
            return false;

        System.err.println("Error in line " + row + ", column " + col + ": " + type + " Redeclaration");
        System.err.println(type + " " + name + " has already been declared on line " + (node.def.row + 1) + ", column " + (node.def.col + 1) + "\n");
        return true;
    }

    public boolean isDefined(String name, int row, int col){
        NodeType node = lookup(name, row, col);
        if (node != null)
            return false;
        return true;
    }

    public int retType(VarExp exp){
        int retType = -1;
        if (exp.variable instanceof SimpleVar){
            NodeType node = nodeExists(((SimpleVar)exp.variable).name);
            retType = node.def.getType();
        } else retType = exp.variable.getType();

        return retType;
    }

    public int varType(VarExp exp){
        int varType = -1;
        NodeType node = null;

        if (exp.variable instanceof SimpleVar) node = nodeExists(((SimpleVar)exp.variable).name);
        else if (exp.variable instanceof IndexVar) node = nodeExists(((IndexVar)exp.variable).name);

        
        if (node != null) return node.def.getType();

        System.err.println("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1) + ": Invalid use of undefined variable " + ((SimpleVar)exp.variable).name + "\n");
        
        return varType;
    }
    
    public int evaluateExp(Exp exp){
        int type = -1;
        if (exp instanceof OpExp){
            int lhsType = evaluateExp(((OpExp) exp).left);
            int rhsType = evaluateExp(((OpExp) exp).right);
            if (!(lhsType == -1 || rhsType == -1))
                if (lhsType != rhsType)
                    System.err.println("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1) + " Incompatible types: " + TYPES[lhsType] + " cannot be converted to " + TYPES[rhsType] + "\n");
                else {
                    type = lhsType;
                    if (((OpExp) exp).op < 5){
                        if (type != 1) {
                            System.err.println("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1) + " performing arithmetic operation on invalid types: " + TYPES[type] + "\n");
                            return -1;
                        }
                    } else {
                        type = 0;
                    }
                }
        } else if (exp instanceof CallExp){
            NodeType node = funcExists(((CallExp) exp).func);

            if (node != null)
                type = ((FunctionDec) node.def).result.typ;

        } else if (exp instanceof VarExp){
            type = varType((VarExp) exp);
        } else type = exp.getType();
        return type;
    }

    public int checkLeftOp(OpExp expLeft){
        int type = -1;
        if (expLeft.right instanceof absyn.VarExp){
            NodeType node = nodeExists(expLeft.right.toString());
            if (node != null)
                type = node.def.getType();
        } else {
            type = expLeft.right.getType();
        }
        return type;
    }

    public int checkCallExp(CallExp exp){
        NodeType node = funcExists(exp.func);
        if (node == null){
            System.err.println("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1) + ": Invalid CallExp to undefined function "  + exp.func + "()\n");
            return -1;
        }

        // Expected params
        VarDecList params = ((FunctionDec) node.def).params;
        if (exp.args == null && params == null){
            return 2;
        } else if (exp.args == null){
            System.err.println("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1) + ": Invalid CallExp sending (VOID) when expecting ("  + params.toString().toUpperCase() + ")\n");
            return -1;
        }

        ExpList expList = (ExpList) exp.args;
        while( params != null && expList != null) {
            int expType = evaluateExp(expList.head);
            NodeType head = nodeExists(expList.head.toString());
            if ((params.head.getType() != expType && expType != -1) || head != null && (head.def instanceof ArrayDec && (head.def.getClass() != params.head.getClass()))){
                System.err.println("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1) + ": Invalid CallExp makes use of " + TYPES[expType] + " when expected: " + params.head.toString().toUpperCase() + "\n");
                return -1;
            }
            expList = expList.tail;
            params = params.tail;
        }
        if (params != null || expList != null) {
            System.err.println("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1) + ": Invalid CallExp argument length\n");
            return -1;
        }

        return ((FunctionDec) node.def).result.typ;
    }

    public void delete(String name) {
        symbolTable.remove(name);
    }

    public void printSymbolTable( int level ) {
        ArrayList<NodeType> scope = symbolTable.get(stack.peek());
        if(scope != null) {
            for( int i = 0; i < scope.size(); i++ ) {
                indent( level );
                System.out.println(scope.get(i).name + ": " + scope.get(i).def.toString());
            }
        }
    }

    public void visit( ArrayDec dec, int level, boolean flag ) {
        if(isDeclared(dec.name, "Array variable", dec.row+1, dec.col+1) == false) {
            if (dec.typ.typ == 2){
                System.err.println("Error in line " + (dec.row + 1) + ", column " + (dec.col + 1) + ": Invalid Array Variable Declaration Type (VOID)");
                System.err.println("Instead expected type (BOOL, INT) got VOID. Changing VOID -> INT\n");
                dec.typ.typ = 1;
            }
            NodeType symbol = new NodeType(dec.name, dec, level);
            insert(stack.peek(), symbol);
        }
    }

    public void visit ( AssignExp exp, int level, boolean flag ) {
        exp.lhs.accept( this, level, flag );
        exp.rhs.accept( this, level, flag );
        int lhsType = varType(exp.lhs);
        int rhsType = evaluateExp(exp.rhs);

        if (lhsType != rhsType && lhsType != -1 && rhsType != -1){
            System.err.println("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1) + " Incompatible types: " + TYPES[lhsType] + " cannot be converted to " + TYPES[rhsType] + "\n");
        }
    }

    public void visit ( BoolExp exp, int level, boolean flag ) {
    }

    public void visit ( CallExp exp, int level, boolean flag ) {
        if (exp.args != null)
            exp.args.accept(this, level, flag);

        checkCallExp(exp);
    }

    public void visit ( CompoundExp exp, int level, boolean flag ) {
        VarDecList varDecList = exp.decs;
        while( varDecList != null ) {
          varDecList.head.accept( this, level, flag );
          varDecList = varDecList.tail;
        }
        if(exp.exps != null)
            exp.exps.accept(this, level, flag);
    }

    public void visit ( DecList decList, int level, boolean flag ) {
        while ( decList != null && decList.head != null) {
            decList.head.accept( this, level, flag );
            decList = decList.tail;
        }
    }

    public void visit ( ExpList expList, int level, boolean flag ) {
        while( expList != null ) {
            if (expList.head != null)
                expList.head.accept( this, level, flag );
            expList = expList.tail;
        }
    }

    public void visit ( FunctionDec dec, int level, boolean flag ) {
        NodeType node = funcExists(dec.func);

        if ((node != null) && (dec.body == null)){
            System.err.println("Error in line " + (dec.row + 1) + ", column " + (dec.col + 1) + ": Function Redeclaration");
            System.err.println("Function " + dec.func + " has already been declared on line " + (node.def.row + 1)+ ", column " + (node.def.col + 1) + "\n");
            return;
        } else if (node != null && ((FunctionDec) node.def).body == null && (dec.body != null)) {
            symbolTable.remove(dec.func);
        }

        level++;
        indent( level );
        System.out.println("Entering the scope for function " + dec.func + ":");
        
        insert("global", new NodeType(dec.func, dec, level));

        level++;
        stack.add(dec.func);
        
        VarDecList varDecList = dec.params;
        while( varDecList != null && varDecList.head != null) {
            varDecList.head.accept( this, level, flag );
            varDecList = varDecList.tail;
        }
        if (dec.body != null)
            dec.body.accept( this, level, flag );
        
        printSymbolTable( level );
        stack.pop();
        level--;

        indent( level );
        System.out.println("Leaving the function scope");
    }

    public void visit ( IfExp exp, int level, boolean flag ) {
        int type = evaluateExp(exp.test);
        if (type != 0 && type != 1){
            System.err.println("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1) + ": Invalid test expression\n");
        }

        indent( level );
        System.out.println("Entering a new block:");

        level++;
        nest++;
        stack.add(String.valueOf(nest));
        
        if (exp.test != null)
            exp.test.accept( this, level, flag );
        if ( exp.then != null)
            exp.then.accept( this, level, flag );
        if ( exp.elsee != null )
            exp.elsee.accept( this, level, flag );
            
        printSymbolTable( level );
        
        delete(String.valueOf(nest));
        nest--;
        stack.pop();
        level--;
            
        indent( level );
        System.out.println("Leaving the block");
    }

    public void visit ( IndexVar var, int level, boolean flag ) {
        var.index.accept( this, level, flag );
        int indexTyp = evaluateExp(var.index);
        if (indexTyp != 1){
            System.err.println("Error in line " + (var.row + 1) + ", column " + (var.col + 1) + ": Invalid array index of type " + TYPES[indexTyp] + " expected INT\n");
        }
    }

    public void visit ( IntExp exp, int level, boolean flag ) {
    }

    public void visit ( NameTy type, int level, boolean flag ) {
    }

    public void visit ( NilExp exp, int level, boolean flag ) {
    }

    public void visit ( OpExp exp, int level, boolean flag ) {
        exp.right.accept( this, level, flag );
        exp.left.accept( this, level, flag );
    }

    public void visit ( ReturnExp expr, int level, boolean flag ) {
        // Get type of function
        NodeType func = symbolTable.get("global").get(symbolTable.get("global").size()-1);
        int funcType = ((FunctionDec) func.def).result.typ;
        expr.exp.accept( this, level, flag );
        int expType = evaluateExp(expr.exp);

        if (funcType != expType && expType != -1)
            System.err.println("Error in line " + (expr.row + 1) + ", column " + (expr.col + 1) + ": Function Type " + TYPES[((FunctionDec) func.def).result.typ] + " cannot return " + TYPES[expType] + "\n");
    }

    public void visit ( SimpleDec dec, int level, boolean flag ) {
        if (isDeclared(dec.name, "Variable", dec.row+1, dec.col+1) == false){
            if (dec.typ.typ == 2){
                System.err.println("Error in line " + (dec.row + 1) + ", column " + (dec.col + 1) + ": Invalid Variable Declaration Type (VOID)");
                System.err.println("Instead expected type (BOOL, INT) got VOID. Changing VOID -> INT\n");
                dec.typ.typ = 1;
            }
            NodeType symbol = new NodeType(dec.name, dec, level);
            insert(stack.peek(), symbol);
        }
    }

    public void visit ( SimpleVar var, int level, boolean flag ) {
        // Check if variable has been declared
        isDefined(var.name, var.row+1, var.col+1);
    }

    public void visit ( VarDecList varDecList, int level, boolean flag ) {
        while( varDecList != null ) {
            varDecList.head.accept( this, level, flag );
            varDecList = varDecList.tail;
        }
    }

    public void visit ( VarExp exp, int level, boolean flag ) {
        exp.variable.accept(this, level, flag);
    }

    public void visit ( WhileExp exp, int level, boolean flag ) {
        int type = evaluateExp(exp.test);
        if (type != 0 && type != 1){
            System.err.println("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1) + ": Invalid test expression\n");
        }

        indent( level );
        System.out.println("Entering a new block:");
        
        level++;
        nest++;
        stack.add(String.valueOf(nest));

        if ( exp.test != null )
            exp.test.accept( this, level, flag );
        exp.body.accept( this, level, flag );

        printSymbolTable( level );

        delete(String.valueOf(nest));
        nest--;
        stack.pop();
        level--;
        
        indent( level );
        System.out.println("Leaving the block");
    }
}