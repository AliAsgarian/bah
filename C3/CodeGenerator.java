import absyn.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public class CodeGenerator implements AbsynVisitor {

    /* Track memory locations */
    private HashMap<String, Integer> varAddresses = new HashMap<>();
    private HashMap<String, Integer> functionAddresses = new HashMap<>();

    /* Special memory offsets */
    private static final int OFP_OFFSET = 0;      // Old frame pointer offset
    private static final int RET_OFFSET = -1;     // Return address offset
    private static final int INIT_OFFSET = -2;    // Initial parameter offset

    /* Global state variables */
    private int mainEntry = -1;          // Entry point for main function
    private int inputEntry;              // Entry point for input function
    private int outputEntry;             // Entry point for output function
    private int globalOffset = 0;        // Offset for global variables
    private int emitLoc = 0;             // Current instruction location
    private int highEmitLoc = 0;         // Highest instruction location
    private int currentFunctionOffset;   // Current function's local variable offset
    private int tempOffset;              // Current temporary variable offset
    private String currentFunction;      // Name of current function being processed
    private List<String> tempVars = new ArrayList<>(); // List of temporary variables

    /* Special registers */
    private static final int AC = 0;    // Accumulator
    private static final int AC1 = 1;   // Secondary accumulator
    private static final int FP = 5;    // Frame pointer
    private static final int GP = 6;    // Global pointer
    private static final int PC = 7;    // Program counter

    public void visit(Absyn trees) {
        // Generate the prelude
        emitComment("Standard prelude:");
        emitRM("LD", GP, 0, AC, "load gp with maxaddress");
        emitRM("LDA", FP, 0, GP, "copy to gp to fp");
        emitRM("ST", AC, 0, AC, "clear location 0");
        
        // Save location for jump around I/O routines
        int jumpLoc = emitSkip(1);
        
        // Input routine
        emitComment("code for input routine");
        inputEntry = emitLoc;
        emitRM("ST", AC, RET_OFFSET, FP, "store return");
        emitRO("IN", AC, 0, 0, "input");
        emitRM("LD", PC, RET_OFFSET, FP, "return to caller");
        
        // Output routine
        emitComment("code for output routine");
        outputEntry = emitLoc;
        emitRM("ST", AC, RET_OFFSET, FP, "store return");
        emitRM("LD", AC, INIT_OFFSET, FP, "load output value");
        emitRO("OUT", AC, 0, 0, "output");
        emitRM("LD", PC, RET_OFFSET, FP, "return to caller");
        
        // Backpatch jump around I/O routines
        int currentLoc = emitLoc;
        emitBackup(jumpLoc);
        emitRM_Abs("LDA", PC, currentLoc, "jump around i/o code");
        emitRestore();
        
        emitComment("End of standard prelude.");
        
        // Process the program
        trees.accept(this, 0, false);
        
        // Check if main was defined
        if (mainEntry == -1) {
            System.err.println("Error: 'main' function not found");
            return;
        }
        
        // Check if main was defined
    if (mainEntry == -1) {
        System.err.println("Error: 'main' function not found");
        return;
    }
    
    // Generate finale
    emitRM("ST", FP, globalOffset + OFP_OFFSET, FP, "push ofp");
    emitRM("LDA", FP, globalOffset, FP, "push frame");
    emitRM("LDA", AC, 1, PC, "load ac with ret ptr");
    emitRM_Abs("LDA", PC, mainEntry, "jump to main loc");
    
    emitComment("End of execution.");
    emitRO("HALT", 0, 0, 0, "");
    }

    public void visit(ArrayDec dec, int offset, boolean isAddr) {
        // Handle array declaration
        if (offset == 0) { // Global array
            emitComment("allocating global var: " + dec.name + "[" + dec.size + "]");
            globalOffset -= dec.size;
            varAddresses.put(dec.name, globalOffset);
        } else { // Local array
            emitComment("processing local var: " + dec.name + "[" + dec.size + "]");
            currentFunctionOffset -= dec.size;
            varAddresses.put(dec.name, currentFunctionOffset);
        }
    }

    public void visit(AssignExp exp, int offset, boolean isAddr) {
        emitComment("-> op");
        
        // Generate address of LHS 
        exp.lhs.accept(this, offset, true);
        int lhsOffset = tempOffset--;
        emitRM("ST", AC, lhsOffset, FP, "op: push left");
        
        // Generate value of RHS
        exp.rhs.accept(this, offset, false);
        
        // Store RHS value to LHS address
        emitRM("LD", AC1, lhsOffset, FP, "op: load left");
        emitRM("ST", AC, 0, AC1, "assign: store value");
        
        emitComment("<- op");
    }

    public void visit(BoolExp exp, int offset, boolean isAddr) {
        emitComment("-> constant");
        emitRM("LDC", AC, exp.value ? 1 : 0, 0, "load bool const");
        emitComment("<- constant");
    }

    public void visit(CallExp exp, int offset, boolean isAddr) {
        emitComment("-> call of function: " + exp.func);
        
        // If it's an input/output function, handle specially
        int functionLoc;
        if (exp.func.equals("input")) {
            functionLoc = inputEntry;
        } else if (exp.func.equals("output")) {
            functionLoc = outputEntry;
        } else if (functionAddresses.containsKey(exp.func)) {
            functionLoc = functionAddresses.get(exp.func);
        } else {
            System.err.println("Error: Undefined function " + exp.func);
            return;
        }
        
        // Process arguments in reverse order
        int argCount = 0;
        if (exp.args != null) {
            ExpList args = exp.args;
            List<Integer> argLocations = new ArrayList<>();
            
            // Save all argument values to temporary locations
            while (args != null) {
                args.head.accept(this, offset, false);
                int argLoc = tempOffset--;
                emitRM("ST", AC, argLoc, FP, "store arg val");
                argLocations.add(argLoc);
                args = args.tail;
                argCount++;
            }
            
            // Copy arguments to the new frame
            for (int i = 0; i < argLocations.size(); i++) {
                emitRM("LD", AC, argLocations.get(i), FP, "load arg");
                emitRM("ST", AC, offset + INIT_OFFSET - i, FP, "store arg val in next frame");
            }
        }
        
        // Set up call frame
        emitRM("ST", FP, offset + OFP_OFFSET, FP, "push ofp");
        emitRM("LDA", FP, offset, FP, "push frame");
        emitRM("LDA", AC, 1, PC, "load ac with ret ptr");
        emitRM_Abs("LDA", PC, functionLoc, "jump to fun loc");
        emitRM("LD", FP, OFP_OFFSET, FP, "pop frame");
        
        emitComment("<- call");
    }

    public void visit(CompoundExp exp, int offset, boolean isAddr) {
        emitComment("-> compound statement");
        
        // Process local variable declarations
        if (exp.decs != null) {
            exp.decs.accept(this, offset, false);
        }
        
        // Process statements
        if (exp.exps != null) {
            exp.exps.accept(this, offset, false);
        }
        
        emitComment("<- compound statement");
    }

    public void visit(DecList decList, int offset, boolean isAddr) {
        while (decList != null) {
            decList.head.accept(this, offset, isAddr);
            decList = decList.tail;
        }
    }

    public void visit(ExpList expList, int offset, boolean isAddr) {
        while (expList != null) {
            if (expList.head != null) {
                expList.head.accept(this, offset, isAddr);
            }
            expList = expList.tail;
        }
    }

    public void visit(FunctionDec dec, int offset, boolean isAddr) {
        emitComment("processing function: " + dec.func);
        
        // Save current function name
        currentFunction = dec.func;
        
        // Jump around function body (will backpatch later)
        emitComment("jump around function body here");
        int jumpLoc = emitSkip(1);
        
        // Store function address
        int functionLoc = emitLoc;
        functionAddresses.put(dec.func, functionLoc);
        
        // If it's main, store its entry point
        if (dec.func.equals("main")) {
            mainEntry = functionLoc;
        }
        
        // Store return address
        emitRM("ST", AC, RET_OFFSET, FP, "store return");
        
        // Reset function offsets
        currentFunctionOffset = INIT_OFFSET;
        tempOffset = currentFunctionOffset - 1;
        
        // Process parameters
        if (dec.params != null) {
            VarDecList params = dec.params;
            int paramOffset = INIT_OFFSET;
            
            while (params != null) {
                if (params.head instanceof SimpleDec) {
                    SimpleDec param = (SimpleDec) params.head;
                    varAddresses.put(param.name, paramOffset);
                } else if (params.head instanceof ArrayDec) {
                    ArrayDec param = (ArrayDec) params.head;
                    varAddresses.put(param.name, paramOffset);
                }
                
                paramOffset--;
                currentFunctionOffset = paramOffset;
                tempOffset = currentFunctionOffset - 1;
                params = params.tail;
            }
        }
        
        // Process function body
        if (dec.body != null) {
            dec.body.accept(this, offset, false);
        }
        
        // Special handling for different function types
        if (dec.func.equals("main")) {
            // Directly halt for main function
            emitRO("HALT", 0, 0, 0, "halt for main function");
        } else {
            // For non-main functions that return a value
            // Ensure return value is in AC
            emitRM("LD", PC, RET_OFFSET, FP, "return to caller");
        }
        
        // Backpatch jump around function
        int savedLoc = emitLoc;
        emitBackup(jumpLoc);
        emitRM_Abs("LDA", PC, savedLoc, "jump around fn body");
        emitRestore();
        
        emitComment("<- fundecl");
    }

    public void visit(IfExp exp, int offset, boolean isAddr) {
        emitComment("-> if");
        
        // Generate test condition
        exp.test.accept(this, offset, false);
        
        // Jump to else part if condition is false
        int falseJump = emitSkip(1);
        
        // Generate then part
        if (exp.then != null) {
            exp.then.accept(this, offset, false);
        }
        
        // Jump around else part (if there is one)
        int elseDone = 0;
        if (exp.elsee != null) {
            elseDone = emitSkip(1);
        }
        
        // Backpatch jump to else part
        int elseLocation = emitLoc;
        emitBackup(falseJump);
        emitRM("JEQ", AC, elseLocation - falseJump, PC, "if: jmp to else");
        emitRestore();
        
        // Generate else part
        if (exp.elsee != null) {
            exp.elsee.accept(this, offset, false);
            
            // Backpatch jump around else part
            int afterElse = emitLoc;
            emitBackup(elseDone);
            emitRM_Abs("LDA", PC, afterElse, "jmp to end");
            emitRestore();
        }
        
        emitComment("<- if");
    }

    public void visit(IndexVar var, int offset, boolean isAddr) {
        emitComment("-> subs");
        
        // Generate index expression
        var.index.accept(this, offset, false);
        int indexLoc = tempOffset--;
        emitRM("ST", AC, indexLoc, FP, "store array index");
        
        // Load array base address
        Integer baseAddr = varAddresses.get(var.name);
        if (baseAddr == null) {
            System.err.println("Error: Undefined array variable " + var.name);
            return;
        }
        
        if (baseAddr >= 0) { // Global array
            emitRM("LDA", AC, baseAddr, GP, "load array base addr");
        } else { // Local array
            emitRM("LDA", AC, baseAddr, FP, "load array base addr");
        }
        
        // Calculate element address: base + index
        emitRM("LD", AC1, indexLoc, FP, "load index");
        emitRO("SUB", AC, AC, AC1, "compute element address");
        
        // If we want the address, we're done; if we want the value, load it
        if (!isAddr) {
            emitRM("LD", AC, 0, AC, "load array element value");
        }
        
        emitComment("<- subs");
    }

    public void visit(IntExp exp, int offset, boolean isAddr) {
        emitComment("-> constant");
        emitRM("LDC", AC, exp.value, 0, "load const");
        emitComment("<- constant");
    }

    public void visit(NameTy type, int offset, boolean isAddr) {
        // Nothing to do here
    }

    public void visit(NilExp exp, int offset, boolean isAddr) {
        // Nothing to do here
    }

    public void visit(OpExp exp, int offset, boolean isAddr) {
        emitComment("-> op");
        
        // Generate left operand
        exp.left.accept(this, offset, false);
        int leftLoc = tempOffset--;
        emitRM("ST", AC, leftLoc, FP, "op: push left");
        
        // Generate right operand
        exp.right.accept(this, offset, false);
        
        // Load left operand
        emitRM("LD", AC1, leftLoc, FP, "op: load left");
        
        // Perform operation
        switch (exp.op) {
            case OpExp.PLUS:
                emitRO("ADD", AC, AC1, AC, "op +");
                break;
                
            case OpExp.MINUS:
                emitRO("SUB", AC, AC1, AC, "op -");
                break;
                
            case OpExp.UMINUS:
                emitRO("SUB", AC, AC, AC, "op unary -");
                break;
                
            case OpExp.TIMES:
                emitRO("MUL", AC, AC1, AC, "op *");
                break;
                
            case OpExp.OVER:
                emitRO("DIV", AC, AC1, AC, "op /");
                break;
                
            case OpExp.EQ:
                emitRO("SUB", AC, AC1, AC, "op ==");
                emitRM("JEQ", AC, 2, PC, "br if true");
                emitRM("LDC", AC, 0, 0, "false case");
                emitRM("LDA", PC, 1, PC, "unconditional jmp");
                emitRM("LDC", AC, 1, 0, "true case");
                break;
                
            case OpExp.NE:
                emitRO("SUB", AC, AC1, AC, "op !=");
                emitRM("JNE", AC, 2, PC, "br if true");
                emitRM("LDC", AC, 0, 0, "false case");
                emitRM("LDA", PC, 1, PC, "unconditional jmp");
                emitRM("LDC", AC, 1, 0, "true case");
                break;
                
            case OpExp.LT:
                emitRO("SUB", AC, AC1, AC, "op <");
                emitRM("JLT", AC, 2, PC, "br if true");
                emitRM("LDC", AC, 0, 0, "false case");
                emitRM("LDA", PC, 1, PC, "unconditional jmp");
                emitRM("LDC", AC, 1, 0, "true case");
                break;
                
            case OpExp.LE:
                emitRO("SUB", AC, AC1, AC, "op <=");
                emitRM("JLE", AC, 2, PC, "br if true");
                emitRM("LDC", AC, 0, 0, "false case");
                emitRM("LDA", PC, 1, PC, "unconditional jmp");
                emitRM("LDC", AC, 1, 0, "true case");
                break;
                
            case OpExp.GT:
                emitRO("SUB", AC, AC1, AC, "op >");
                emitRM("JGT", AC, 2, PC, "br if true");
                emitRM("LDC", AC, 0, 0, "false case");
                emitRM("LDA", PC, 1, PC, "unconditional jmp");
                emitRM("LDC", AC, 1, 0, "true case");
                break;
                
            case OpExp.GE:
                emitRO("SUB", AC, AC1, AC, "op >=");
                emitRM("JGE", AC, 2, PC, "br if true");
                emitRM("LDC", AC, 0, 0, "false case");
                emitRM("LDA", PC, 1, PC, "unconditional jmp");
                emitRM("LDC", AC, 1, 0, "true case");
                break;
                
            default:
                System.err.println("Error: Unrecognized operator " + exp.op);
        }
        
        emitComment("<- op");
    }

    public void visit(ReturnExp expr, int offset, boolean isAddr) {
        emitComment("-> return");
        
        // Process return expression if there is one
        if (expr.exp != null && !(expr.exp instanceof NilExp)) {
            expr.exp.accept(this, offset, false);
        }
        
        // Special handling for main function
        if (currentFunction.equals("main")) {
            // Directly jump to HALT instruction
            emitRM_Abs("LDA", PC, emitLoc + 2, "jump to HALT for main");
        } else {
            // Normal return for other functions
            emitRM("LD", PC, RET_OFFSET, FP, "return to caller");
        }
        
        emitComment("<- return");
    }

    public void visit(SimpleDec dec, int offset, boolean isAddr) {
        // Handle simple variable declaration
        if (offset == 0) { // Global variable
            emitComment("allocating global var: " + dec.name);
            globalOffset--;
            varAddresses.put(dec.name, globalOffset);
        } else { // Local variable
            emitComment("processing local var: " + dec.name);
            currentFunctionOffset--;
            varAddresses.put(dec.name, currentFunctionOffset);
        }
    }

    public void visit(SimpleVar var, int offset, boolean isAddr) {
        emitComment("-> id");
        emitComment("looking up id: " + var.name);
        
        // Find variable address
        Integer varAddr = varAddresses.get(var.name);
        if (varAddr == null) {
            System.err.println("Error: Undefined variable " + var.name);
            return;
        }
        
        if (varAddr >= 0) { // Global variable
            if (isAddr) {
                emitRM("LDA", AC, varAddr, GP, "load id address");
            } else {
                emitRM("LD", AC, varAddr, GP, "load id value");
            }
        } else { // Local variable
            if (isAddr) {
                emitRM("LDA", AC, varAddr, FP, "load id address");
            } else {
                emitRM("LD", AC, varAddr, FP, "load id value");
            }
        }
        
        emitComment("<- id");
    }

    public void visit(VarDecList varDecList, int offset, boolean isAddr) {
        while (varDecList != null) {
            varDecList.head.accept(this, offset, isAddr);
            varDecList = varDecList.tail;
        }
    }

    public void visit(VarExp exp, int offset, boolean isAddr) {
        exp.variable.accept(this, offset, isAddr);
    }

    public void visit(WhileExp exp, int offset, boolean isAddr) {
        emitComment("-> while");
        
        // Save location of test
        int testLoc = emitLoc;
        
        // Generate code for test expression
        exp.test.accept(this, offset, false);
        
        // Jump to end if test is false (will backpatch later)
        int falseJump = emitSkip(1);
        
        // Generate code for body
        exp.body.accept(this, offset, false);
        
        // Jump back to test
        emitRM_Abs("LDA", PC, testLoc, "while: absolute jmp to test");
        
        // Backpatch jump to end
        int afterWhile = emitLoc;
        emitBackup(falseJump);
        emitRM_Abs("JEQ", AC, afterWhile, "while: jmp to end");
        emitRestore();
        
        emitComment("<- while");
    }

    // Emit helper methods
    private void emitComment(String comment) {
        System.out.println("* " + comment);
    }

    private void emitRO(String op, int r, int s, int t, String comment) {
        System.out.printf("%3d: %5s %d,%d,%d \t%s\n", emitLoc, op, r, s, t, comment);
        emitLoc++;
        if (highEmitLoc < emitLoc) highEmitLoc = emitLoc;
    }

    private void emitRM(String op, int r, int d, int s, String comment) {
        System.out.printf("%3d: %5s %d,%d(%d) \t%s\n", emitLoc, op, r, d, s, comment);
        emitLoc++;
        if (highEmitLoc < emitLoc) highEmitLoc = emitLoc;
    }

    private void emitRM_Abs(String op, int r, int a, String comment) {
        System.out.printf("%3d: %5s %d,%d(%d) \t%s\n", emitLoc, op, r, a - (emitLoc + 1), PC, comment);
        emitLoc++;
        if (highEmitLoc < emitLoc) highEmitLoc = emitLoc;
    }

    private int emitSkip(int distance) {
        int i = emitLoc;
        emitLoc += distance;
        if (highEmitLoc < emitLoc) highEmitLoc = emitLoc;
        return i;
    }

    private void emitBackup(int loc) {
        if (loc > highEmitLoc) emitComment("BUG in emitBackup");
        emitLoc = loc;
    }

    private void emitRestore() {
        emitLoc = highEmitLoc;
    }
}