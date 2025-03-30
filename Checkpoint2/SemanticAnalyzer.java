import absyn.*;
import java.util.HashMap;
import java.util.Stack;
import java.util.Iterator;
import java.util.ArrayList;

public class SemanticAnalyzer implements AbsynVisitor {

    // Symbol table to store variable/function declarations in different scopes
    public HashMap<String, ArrayList<NodeType>> symbolTable;

    // Stack to track scope hierarchy
    public Stack<String> stack;

    // Keeps track of nesting level of scopes
    public int nest = 0;

    // Allowed data types
    public String[] TYPES = {"BOOL", "INT", "VOID"};

    public SemanticAnalyzer() {
        // Initialize symbol table and scope tracking stack
        symbolTable = new HashMap<String, ArrayList<NodeType>>();
        stack = new Stack<String>();

        // Start with the global scope
        stack.add("global");
    }

    // Helper function to print indentation based on nesting level (for debugging/logging)
    private void AddIndent(int level) {
        for (int i = 0; i < level * 4; i++) {
            System.out.print(" ");
        }
    }

    // Inserts a node (variable or function) into the symbol table
    public void insertNode(String key, NodeType node) {
        ArrayList<NodeType> nodeList = symbolTable.get(key);

        // If the key (variable/function) is not in the symbol table, create a new entry
        if (nodeList == null) {
            nodeList = new ArrayList<NodeType>();
            nodeList.add(node);
            symbolTable.put(stack.peek(), nodeList); // Associate with the current scope
        } 
        else {
            // If the node is not already in the list, add it
            if (!nodeList.contains(node)) {
                nodeList.add(node);
            }
        }
    }

    // Looks up a node in the symbol table, checking if it exists in any scope
    public NodeType lookupNode(String name) {
        return nodeExists(name);
    }

    // Deletes a variable or function from the symbol table
    public void delete(String name) {
        symbolTable.remove(name);
    }

    // Checks if a function exists in the global scope
    public NodeType funcExists(String name) {
        ArrayList<NodeType> list = symbolTable.get("global");

        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).name.equals(name)) {
                    return list.get(i); // Function found
                }
            }
        }
        return null; // Function does not exist
    }

    // Checks if a variable or function exists in the current or global scope
    public NodeType nodeExists(String name) {
        Iterator<String> scope = stack.iterator();

        // Skip the global scope if we are in a nested scope
        if (!stack.peek().equals("global"))
            scope.next();

        // Iterate through scopes (from current to global)
        while (scope.hasNext()) {
            ArrayList<NodeType> list = symbolTable.get(scope.next());
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).name.equals(name)) {
                        return list.get(i); // Variable or function found
                    }
                }
            }
        }

        // If not found in the current scope, check the global scope as a last resort
        if (!stack.peek().equals("global")) {
            ArrayList<NodeType> list = symbolTable.get("global");
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).name.equals(name)) {
                        return list.get(i); // Found in the global scope
                    }
                }
            }
        }
        return null; // Not found
    }

    // Checks if a variable or function has already been declared
    public boolean isDeclared(String name, String type, int row, int col) {
        NodeType node = lookupNode(name);

        // If the node does not exist or is a global variable (level 0), it is not declared yet
        if (node == null || node.level == 0)
            return false;

        // Otherwise, print an error message about the redeclaration
        System.err.println("Error on line " + row + ", column " + col + ": ");
        System.err.println(type + " " + name + " has already been declared on line " 
            + (node.def.row + 1) + ", column " + (node.def.col + 1) + "\n");

        return true; // The name has already been declared
    }

    // Checks if a function or variable is defined (exists in the symbol table)
    public boolean isDefined(String name) {
        NodeType node = lookupNode(name);
        return node == null; // If node does not exist, it is not defined
    }

    // Returns the return type of a variable expression (VarExp)
    public int retType(VarExp exp) {
        int retType = -1;

        // If the variable is a simple variable (not an array or indexed variable)
        if (exp.variable instanceof SimpleVar) {
            // Look up the node in the symbol table
            NodeType node = nodeExists(((SimpleVar) exp.variable).name);
            // Retrieve the type of the variable from its definition
            retType = node.def.getType();
        } 
        else {
            // If it's another type of variable, get its type directly
            retType = exp.variable.getType();
        }

        return retType;
    }

    // Returns the type of a variable expression (VarExp)
    public int varType(VarExp exp) {
        int varType = -1; // Default to -1 if variable not found
        NodeType node = null;

        // Determine if the variable is a simple variable or an indexed variable (array)
        if (exp.variable instanceof SimpleVar) {
            node = nodeExists(((SimpleVar) exp.variable).name);
        } 
        else if (exp.variable instanceof IndexVar) {
            node = nodeExists(((IndexVar) exp.variable).name);
        }

        // If the variable exists, return its type
        if (node != null) return node.def.getType();

        // Otherwise, print an error message indicating an undefined variable
        System.err.println("Error on line " + (exp.row + 1) + ", column " + (exp.col + 1) +
            ": This variable is not defined " + ((SimpleVar) exp.variable).name + "\n");

        return varType;
    }

    // Evaluates the type of an expression (Exp)
    public int evaluateExp(Exp exp) {
        int type = -1; // Default to -1 (unknown type)

        if (exp instanceof OpExp) {
            // If the expression is an operation, evaluate its left and right operands
            int lhsType = evaluateExp(((OpExp) exp).left);
            int rhsType = evaluateExp(((OpExp) exp).right);

            // Check for type mismatches in binary operations
            if (!(lhsType == -1 || rhsType == -1)) {
                if (lhsType != rhsType) {
                    System.err.println("Error on line " + (exp.row + 1) + ", column " + (exp.col + 1) +
                        " Incompatible types: " + TYPES[lhsType] + " cannot be converted to " + TYPES[rhsType] + "\n");
                } 
                else {
                    type = lhsType;

                    // If the operator is arithmetic (< 5), ensure both operands are integers
                    if (((OpExp) exp).op < 5) {
                        if (type != 1) { // 1 represents INT type
                            System.err.println("Error on line " + (exp.row + 1) + ", column " + (exp.col + 1) +
                                " performing arithmetic operation on invalid types: " + TYPES[type] + "\n");
                            return -1;
                        }
                    } 
                    else {
                        // For logical/comparison operators, the result is always boolean
                        type = 0; // 0 represents BOOL type
                    }
                }
            }
        } 
        else if (exp instanceof CallExp) {
            // If it's a function call, check if the function exists
            NodeType node = funcExists(((CallExp) exp).func);

            if (node != null) {
                // Return the function's return type
                type = ((FunctionDec) node.def).result.typ;
            }
        } 
        else if (exp instanceof VarExp) {
            // If it's a variable expression, get its type
            type = varType((VarExp) exp);
        } 
        else {
            // Otherwise, return the type directly from the expression
            type = exp.getType();
        }

        return type;
    }

    // Checks the type of the right-hand operand in an operation expression
    public int checkLeftOp(OpExp expLeft) {
        int type = -1;

        // If the right operand is a variable expression
        if (expLeft.right instanceof absyn.VarExp) {
            NodeType node = nodeExists(expLeft.right.toString());

            if (node != null) {
                type = node.def.getType();
            }
        } 
        else {
            // Otherwise, get the type of the right operand directly
            type = expLeft.right.getType();
        }

        return type;
    }

    // Checks the validity of a function call expression (CallExp) and its argument types
    public int checkCallExp(CallExp exp) {
        // Look up the function in the global symbol table
        NodeType node = funcExists(exp.func);

        if (node == null) {
            // If the function does not exist, print an error message
            System.err.println("Error on line " + (exp.row + 1) + ", column " + (exp.col + 1) +
                ": Invalid CallExp to undefined function " + exp.func + "()\n");
            return -1;
        }

        // Get the expected parameters from the function definition
        VarDecList params = ((FunctionDec) node.def).params;

        // If the function has no parameters and no arguments are provided, it's valid
        if (exp.args == null && params == null) {
            return 2; // 2 could represent a valid function call
        } 
        else if (exp.args == null) {
            // If no arguments are provided but parameters are expected, print an error
            System.err.println("Error on line " + (exp.row + 1) + ", column " + (exp.col + 1) +
                ": Invalid CallExp sending (VOID) when expecting (" + params.toString().toUpperCase() + ")\n");
            return -1;
        }

        // Iterate over the arguments provided in the function call
        ExpList expList = (ExpList) exp.args;
        while (params != null && expList != null) {
            int expType = evaluateExp(expList.head);

            // Check if the expected type matches the provided argument type
            NodeType head = nodeExists(expList.head.toString());

            if ((params.head.getType() != expType && expType != -1) ||
                (head != null && (head.def instanceof ArrayDec && (head.def.getClass() != params.head.getClass())))) {

                System.err.println("Error on line " + (exp.row + 1) + ", column " + (exp.col + 1) +
                    ": Invalid CallExp makes use of " + TYPES[expType] + " when expected: " +
                    params.head.toString().toUpperCase() + "\n");
                return -1;
            }

            // Move to the next argument and parameter
            expList = expList.tail;
            params = params.tail;
        }

        // If there are extra arguments or missing arguments, print an error
        if (params != null || expList != null) {
            System.err.println("Error on line " + (exp.row + 1) + ", column " + (exp.col + 1) +
                ": Invalid CallExp argument length\n");
            return -1;
        }

        // Return the function's return type
        return ((FunctionDec) node.def).result.typ;
    }




    // Prints the symbol table for the current scope
    public void printSymbolTable(int level) {
        ArrayList<NodeType> scope = symbolTable.get(stack.peek());
        if (scope != null) {
            for (int i = 0; i < scope.size(); i++) {
                AddIndent(level); // Indent based on the nesting level for readability
                System.out.println(scope.get(i).name + ": " + scope.get(i).def.toString());
            }
        }
    }

    // Visits an Array Variable Declaration (ArrayDec) and adds it to the symbol table
    public void visit(ArrayDec dec, int level) {
        // Check if the array variable has already been declared
        if (!isDeclared(dec.name, "Array variable", dec.row + 1, dec.col + 1)) {
            // Ensure the type is not VOID (type = 2)
            if (dec.typ.typ == 2) {
                System.err.println("Error on line " + (dec.row + 1) + ", column " + (dec.col + 1) +
                    ": Invalid Array Variable Declaration Type (VOID)");
                System.err.println("Instead expected type (BOOL, INT) got VOID. Changing to INT\n");
                dec.typ.typ = 1; // Change type to INT (type = 1)
            }
            // Add the array variable to the symbol table
            NodeType symbol = new NodeType(dec.name, dec, level);
            insertNode(stack.peek(), symbol);
        }
    }

    // Visits an Assignment Expression (AssignExp) and ensures type compatibility
    public void visit(AssignExp exp, int level) {
        // Visit left-hand side and right-hand side expressions
        exp.lhs.accept(this, level);
        exp.rhs.accept(this, level);

        // Get types of left-hand side and right-hand side expressions
        int lhsType = varType(exp.lhs);
        int rhsType = evaluateExp(exp.rhs);

        // Check if types are incompatible
        if (lhsType != rhsType && lhsType != -1 && rhsType != -1) {
            System.err.println("Error on line " + (exp.row + 1) + ", column " + (exp.col + 1) +
                " Incompatible types: " + TYPES[lhsType] + " cannot be converted to " + TYPES[rhsType] + "\n");
        }
    }

    // Visits a Boolean Expression (BoolExp), does nothing in this case
    public void visit(BoolExp exp, int level) {
        // No semantic checking needed for simple boolean expressions
    }

    // Visits a Function Call Expression (CallExp)
    public void visit(CallExp exp, int level) {
        // If the function has arguments, process them first
        if (exp.args != null) {
            exp.args.accept(this, level);
        }
        // Check the validity of the function call
        checkCallExp(exp);
    }

    // Visits a Compound Expression (CompoundExp), which consists of variable declarations and expressions
    public void visit(CompoundExp exp, int level) {
        // Process variable declarations
        VarDecList varDecList = exp.decs;
        while (varDecList != null) {
            varDecList.head.accept(this, level);
            varDecList = varDecList.tail;
        }
        // Process expressions inside the compound statement
        if (exp.exps != null) {
            exp.exps.accept(this, level);
        }
    }

    // Visits a List of Declarations (DecList) and processes each declaration
    public void visit(DecList decList, int level) {
        while (decList != null && decList.head != null) {
            decList.head.accept(this, level);
            decList = decList.tail;
        }
    }

    // Visits a List of Expressions (ExpList) and processes each expression
    public void visit(ExpList expList, int level) {
        while (expList != null) {
            if (expList.head != null) {
                expList.head.accept(this, level);
            }
            expList = expList.tail;
        }
    }

    // Visits a Function Declaration (FunctionDec) and processes its scope, parameters, and body
    public void visit(FunctionDec dec, int level) {
        // Check if the function already exists
        NodeType node = funcExists(dec.func);

        if (node != null && dec.body == null) {
            // Error: Function has been declared before
            System.err.println("Error on line " + (dec.row + 1) + ", column " + (dec.col + 1));
            System.err.println("Function " + dec.func + " has already been declared on line " +
                (node.def.row + 1) + ", column " + (node.def.col + 1) + "\n");
            return;
        } 
        else if (node != null && ((FunctionDec) node.def).body == null && dec.body != null) {
            // If function was previously declared without a body but now has one, remove old entry
            symbolTable.remove(dec.func);
        }

        // Increase the scope level
        level++;
        AddIndent(level);
        System.out.println("Entering the scope for function " + dec.func + ":");

        // Insert the function into the global scope
        insertNode("global", new NodeType(dec.func, dec, level));

        // Create a new scope for the function
        level++;
        stack.add(dec.func);

        // Process function parameters
        VarDecList varDecList = dec.params;
        while (varDecList != null && varDecList.head != null) {
            varDecList.head.accept(this, level);
            varDecList = varDecList.tail;
        }

        // Process function body
        if (dec.body != null) {
            dec.body.accept(this, level);
        }

        // Print the function's symbol table
        printSymbolTable(level);
        
        // Exit the function scope
        stack.pop();
        level--;

        AddIndent(level);
        System.out.println("Leaving the function scope");
    }

    // Visits an If Expression (IfExp), ensuring test expression is valid and handling scope changes
    public void visit(IfExp exp, int level) {
        // Evaluate the test condition
        int type = evaluateExp(exp.test);

        // The test condition must be of type BOOL (0) or INT (1), otherwise it's an error
        if (type != 0 && type != 1) {
            System.err.println("Error on line " + (exp.row + 1) + ", column " + (exp.col + 1) +
                ": Invalid test expression\n");
        }

        AddIndent(level);
        System.out.println("Entering a new block:");

        // Increase nesting level for new block
        level++;
        nest++;
        stack.add(String.valueOf(nest)); // New scope ID

        // Visit test condition, then block, and else block (if it exists)
        if (exp.test != null) exp.test.accept(this, level);
        if (exp.then != null) exp.then.accept(this, level);
        if (exp.elsee != null) exp.elsee.accept(this, level);

        // Print the symbol table for debugging
        printSymbolTable(level);

        // Remove scope after leaving the block
        delete(String.valueOf(nest));
        nest--;
        stack.pop();
        level--;

        AddIndent(level);
        System.out.println("Leaving the block");
    }

    // Visits an Indexed Variable (IndexVar) and ensures the index is of type INT
    public void visit(IndexVar var, int level) {
        var.index.accept(this, level);
        int indexTyp = evaluateExp(var.index);

        // Array index must be of type INT (1), otherwise it's an error
        if (indexTyp != 1) {
            System.err.println("Error on line " + (var.row + 1) + ", column " + (var.col + 1) +
                ": Invalid array index of type " + TYPES[indexTyp] + ", expected INT\n");
        }
    }

    // Visits an Integer Expression (IntExp) - No processing needed
    public void visit(IntExp exp, int level) {
    }

    // Visits a Type Name (NameTy) - No processing needed
    public void visit(NameTy type, int level) {
    }

    // Visits a Nil Expression (NilExp) - No processing needed
    public void visit(NilExp exp, int level) {
    }

    // Visits an Operator Expression (OpExp) and evaluates left and right operands
    public void visit(OpExp exp, int level) {
        exp.right.accept(this, level);
        exp.left.accept(this, level);
    }

    // Visits a Return Expression (ReturnExp) and ensures the return type matches the function's type
    public void visit(ReturnExp expr, int level) {
        // Retrieve function's return type from the global symbol table
        NodeType func = symbolTable.get("global").get(symbolTable.get("global").size() - 1);
        int funcType = ((FunctionDec) func.def).result.typ;

        // Process the return expression
        expr.exp.accept(this, level);
        int expType = evaluateExp(expr.exp);

        // Check if the return type matches the function return type
        if (funcType != expType && expType != -1) {
            System.err.println("Error on line " + (expr.row + 1) + ", column " + (expr.col + 1) +
                ": Function Type " + TYPES[funcType] + " cannot return " + TYPES[expType] + "\n");
        }
    }

    // Visits a Simple Variable Declaration (SimpleDec) and adds it to the symbol table
    public void visit(SimpleDec dec, int level) {
        // Ensure the variable hasn't already been declared
        if (!isDeclared(dec.name, "Variable", dec.row + 1, dec.col + 1)) {
            // Ensure the variable isn't declared as VOID
            if (dec.typ.typ == 2) {
                System.err.println("Error on line " + (dec.row + 1) + ", column " + (dec.col + 1) +
                    ": Invalid Variable Declaration Type (VOID)");
                System.err.println("Instead expected type (BOOL, INT) got VOID. Changing to INT\n");
                dec.typ.typ = 1; // Convert VOID to INT
            }
            // Add the variable to the symbol table
            NodeType symbol = new NodeType(dec.name, dec, level);
            insertNode(stack.peek(), symbol);
        }
    }

    // Visits a Simple Variable (SimpleVar) and ensures it has been defined
    public void visit(SimpleVar var, int level) {
        isDefined(var.name); // Check if the variable is defined
    }

    // Visits a List of Variable Declarations (VarDecList) and processes each declaration
    public void visit(VarDecList varDecList, int level) {
        while (varDecList != null) {
            varDecList.head.accept(this, level);
            varDecList = varDecList.tail;
        }
    }

    // Visits a Variable Expression (VarExp) and processes the variable
    public void visit(VarExp exp, int level) {
        exp.variable.accept(this, level);
    }

    // Visits a While Expression (WhileExp) and ensures test expression is valid
    public void visit(WhileExp exp, int level) {
        int type = evaluateExp(exp.test);

        // The test condition must be of type BOOL (0) or INT (1)
        if (type != 0 && type != 1) {
            System.err.println("Error on line " + (exp.row + 1) + ", column " + (exp.col + 1) +
                ": Invalid test expression\n");
        }

        AddIndent(level);
        System.out.println("Entering a new block:");

        // Increase nesting level for loop body
        level++;
        nest++;
        stack.add(String.valueOf(nest));

        // Visit test condition and loop body
        if (exp.test != null) exp.test.accept(this, level);
        exp.body.accept(this, level);

        // Print symbol table for debugging
        printSymbolTable(level);

        // Remove scope after leaving the loop body
        delete(String.valueOf(nest));
        nest--;
        stack.pop();
        level--;

        AddIndent(level);
        System.out.println("Leaving the block");
    }
}