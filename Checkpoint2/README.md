# **Checkpoint 2 - README**

## **Acknowledgements**
This project was built using and inspired by files from **Professor Fei Song's C1-Package**.

---

## **How to Build**

To compile the compiler, run:
```
make
```

To clean up generated files:
```
make clean
```

---

## **How to Run the Compiler**

To run the compiler on a test file:
```
java -cp /usr/share/java/cup.jar:. Main test/filename.cm
```

To run the compiler with symbol table display (`-s` option):
```
java -cp /usr/share/java/cup.jar:. Main test/filename.cm -s
```

To run the compiler with abstract syntax tree display (`-a` option):
```
java -cp /usr/share/java/cup.jar:. Main test/filename.cm -a
```

---

## **Test Instructions**

### **1.cm - Valid Program**
**Command:**
```
java -cp /usr/share/java/cup.jar:. Main test/1.cm
```
**Expected Output:**
- This program should compile successfully without errors.
- It demonstrates variable declarations, assignments, and conditional logic.

---

### **2.cm - Semantic Error (Type Mismatch & Undefined Function)**
**Command:**
```
java -cp /usr/share/java/cup.jar:. Main test/2.cm
```
**Expected Output:**
- Lexical error on line 5: Unrecognized character $.
- Syntax error on line 6, column 12: Unexpected argument before semicolon.
- Lexical error on line 9: Unrecognized character $.
- Syntax error on line 10, column 8: Invalid variable declaration, unexpected token before semicolon.

---

### **3.cm - Semantic Error (Invalid Array Index & Function Call)**
**Command:**
```
java -cp /usr/share/java/cup.jar:. Main test/3.cm
```
**Expected Output:**
- Syntax error on line 7, column 3: Invalid variable declaration, unexpected argument before semicolon.

---

### **4.cm - Semantic Error (Variable Redeclaration & Undefined Variable)**
**Command:**
```
java -cp /usr/share/java/cup.jar:. Main test/4.cm
```
**Expected Output:**
- Syntax error on line 16, column 3: Invalid variable declaration, unexpected argument before semicolon.
---

### **5.cm - Multiple Errors (Invalid Declarations, Return Type Mismatch, and Function Calls)**
**Command:**
```
java -cp /usr/share/java/cup.jar:. Main test/5.cm
```
**Expected Output:**
- Syntax error on line 10, column 8: Invalid variable declaration, unexpected argument before semicolon.
- General errors in type usage and naming conventions.





