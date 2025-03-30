import absyn.*;

public class ShowTreeVisitor implements AbsynVisitor {

    final static int SPACES = 4;

    private void indent( int level ) {
        for( int i = 0; i < level * SPACES; i++ ) System.out.print( " " );
    }
    
    public void visit( ArrayDec dec, int level ) {
        indent( level );
        level++;
        System.out.println( "ArrayDec:");
        indent( level );
        if ( dec.typ.typ == 1) {
            System.out.println( "NameTy: int" );
            indent( level );
            System.out.println( "Name: " + dec.name );
            if( dec.size != 0 ) {
                indent( level );
                System.out.println( "Size: " + dec.size );
            }
        }
    }

    public void visit ( AssignExp exp, int level ) {
        indent( level );
        System.out.println( "AssignExp:" );
        level++;
        exp.lhs.accept( this, level );
        exp.rhs.accept( this, level );
    }

    public void visit ( BoolExp exp, int level ) {
        indent( level );
        level++;
        System.out.println( "BoolExp: " + exp.value );
    }

    public void visit ( CallExp exp, int level ) {
        indent( level );
        System.out.println( "CallExp: " );
        level++;
        indent( level );
        System.out.println("Func: " + exp.func);
        ExpList argsList = exp.args;
        while( argsList != null ) {
          argsList.head.accept( this, level );
          argsList = argsList.tail;
        }
    }

    public void visit ( CompoundExp exp, int level ) {
        indent( level );
        System.out.println( "CompoundExp: " );
        level++;
        VarDecList varDecList = exp.decs;
        while( varDecList != null ) {
          varDecList.head.accept( this, level );
          varDecList = varDecList.tail;
        }
        ExpList expList = exp.exps;
        while( expList != null ) {
            expList.head.accept( this, level );
            expList = expList.tail;
        }
    }

    public void visit ( DecList decList, int level ) {
        while ( decList != null && decList.head != null) {
            decList.head.accept( this, level );
            decList = decList.tail;
        }
    }

    public void visit ( ExpList expList, int level ) {
        while( expList != null ) {
            expList.head.accept( this, level );
            expList = expList.tail;
        } 
    }

    public void visit ( FunctionDec dec, int level ) {
        indent( level );
        System.out.println("FunctionDec:");
        level++;
        
        indent( level );
        if( dec.result.typ == 0 )
            System.out.println( "NameTy: bool" );
        else if ( dec.result.typ == 1 )
            System.out.println( "NameTy: int");
        else if ( dec.result.typ == 2 )
            System.out.println( "NameTy: void");

        VarDecList varDecList = dec.params;
        while( varDecList != null && varDecList.head != null) {
            varDecList.head.accept( this, level );
            varDecList = varDecList.tail;
        }
        if( dec.body != null )
            dec.body.accept( this, level );   
    }

    public void visit ( IfExp exp, int level ) {
        indent( level );
        System.out.println( "IfExp:" );
        level++;
        if (exp.test != null)
            exp.test.accept( this, level );
        if ( exp.then != null)
            exp.then.accept( this, level );
        if ( exp.elsee != null )
           exp.elsee.accept( this, level );
    }

    public void visit ( IndexVar var, int level ) {
        indent( level );
        System.out.println( "IndexVar: " + var.name );
        level++;
        var.index.accept( this, level );
    }
    
    public void visit ( IntExp exp, int level ) {
        indent( level );
        System.out.println( "IntExp: " + exp.value ); 
    }

    public void visit ( NameTy type, int level ) {
        indent( level );
        if ( type.typ == 0) 
            System.out.println( "NameTy: BOOL" );
        else if ( type.typ == 1 ) 
            System.out.println( "NameTy: INT" );
        else if ( type.typ == 2 ) 
            System.out.println( "NameTy: VOID" );
    }
    
    public void visit ( NilExp exp, int level ) {
        indent( level );
        System.out.println( "NilExp: null" );
    }

    public void visit ( OpExp exp, int level ) {
        indent( level );
        switch( exp.op ) {
            case OpExp.PLUS:
                System.out.println( "OpExp: + " );
                break;
            case OpExp.MINUS:
                System.out.println( "OpExp: - " );
                break;
            case OpExp.UMINUS:
                System.out.println( "OpExp: - " );
                break;
            case OpExp.TIMES:
                System.out.println( "OpExp: * " );
                break;
            case OpExp.OVER:
                System.out.println( "OpExp: / " );
                break;
            case OpExp.EQ:
                System.out.println( "OpExp: == " );
                break;
            case OpExp.NE:
                System.out.println( "OpExp: != " );
                break;
            case OpExp.LT:
                System.out.println( "OpExp: < " );
                break;
            case OpExp.LE:
                System.out.println( "OpExp: <= " );
                break;
            case OpExp.GT:
                System.out.println( "OpExp: > " );
                break;
            case OpExp.GE:
                System.out.println( "OpExp: >= " );
                break;
            case OpExp.NOT:
                System.out.println( "OpExp: ~ " );
                break;
            case OpExp.AND:
                System.out.println( "OpExp: && " );
                break;
            case OpExp.OR:
                System.out.println( "OpExp: || " );
                break;
            default:
                System.out.println( "Unrecognized operator at line " + exp.row + " and column " + exp.col );
        }
        level++;
        if ( exp.left != null )
           exp.left.accept( this, level );
        exp.right.accept( this, level );
    }

    public void visit ( ReturnExp expr, int level ) {
        indent( level );
        System.out.println( "ReturnExp:" );
        level++;
        if ( expr.exp != null )
            expr.exp.accept( this, level );
    }

    public void visit ( SimpleDec dec, int level ) {
        indent( level );

        if (dec.name == null) return;

        System.out.println( "SimpleDec:");
        level++;
    
        indent( level );
        if( dec.typ.typ == 0 )
            System.out.println("NameTy: bool");
        else if( dec.typ.typ == 1 ) 
            System.out.println( "NameTy: int");
        else if( dec.typ.typ == 2 )
            System.out.println( "NameTy: void"); 

        indent( level );
        System.out.println("String: " + dec.name);
    }

    public void visit ( SimpleVar var, int level ) {
        indent( level );
        System.out.println( "SimpleVar: " + var.name );
    }

    public void visit ( VarDecList varDecList, int level ) {
        while( varDecList != null ) {
            varDecList.head.accept( this, level );
            varDecList = varDecList.tail;
        } 
    }

    public void visit ( VarExp exp, int level ) {
        indent( level );
        System.out.println( "VarExp:" );
        level++;
        exp.variable.accept( this, level );
    }

    public void visit ( WhileExp exp, int level ) {
        indent( level );
        System.out.println( "WhileExp:" );
        level++;
        if ( exp.test != null )
            exp.test.accept( this, level );
        exp.body.accept( this, level );
    }
}
