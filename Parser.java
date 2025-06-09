import java.io.PrintWriter;

public class Parser {
    private PrintWriter writer;
    private PrintWriter vmWriter;
    CodeWriter codeWriter;
    private int indent;
    
    private SymbolTable symbolTable = new SymbolTable(); 
    private String className;
    private String functionName;
    private String subroutineType;

    private int labelCounter = 0;

    public Parser(PrintWriter writer, PrintWriter vmWriter){
        this.writer = writer;
        this.vmWriter = vmWriter;
        this.indent = 0;
        this.codeWriter = new CodeWriter(vmWriter);
    }

    public void outputTokenXml(String token, String type){
        for (int i = 0; i < indent; i++) {
            writer.print("  ");
        }
        writer.println("<"+type+"> "+token+" </"+type+">");
        
    }

    public void compileClass(){
        writeStructuralTag("class", false);

        if(Tokenizer.hasMoreTokens()){
            Tokenizer.advance();
        }else {
            System.err.println("Error: No tokens found in file for parsing.");
            return;
        }

        if (Tokenizer.tokenType().equals("keyword") && Tokenizer.tokenValue().equals("class")) {
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        } else {
            System.err.println("Syntax Error: Expected 'class' keyword. Found: '" + Tokenizer.tokenValue() + "' (" + Tokenizer.tokenType() + ")");
            return;
        }

        if (Tokenizer.tokenType().equals("identifier")) {
            className = Tokenizer.tokenValue();
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        } else {
            System.err.println("Syntax Error: Expected class name (identifier). Found: '" + Tokenizer.tokenValue() + "' (" + Tokenizer.tokenType() + ")");
            return;
        }

        if (Tokenizer.tokenType().equals("symbol") && Tokenizer.tokenValue().equals("{")) {
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        } else {
            System.err.println("Syntax Error: Expected '{' after class name. Found: '" + Tokenizer.tokenValue() + "' (" + Tokenizer.tokenType() + ")");
            return;
        }

        while(Tokenizer.tokenType().equals("keyword") &&(Tokenizer.tokenValue().equals("static") || 
            Tokenizer.tokenValue().equals("field"))){
            compileClassVarDec();
        }

        while(Tokenizer.tokenValue().equals("function") || Tokenizer.tokenValue().equals("method") || Tokenizer.tokenValue().equals("constructor")){
            compileSubroutineDec();
        }

        if (Tokenizer.tokenValue().equals("}")) {
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }

        writeStructuralTag("class", true);
    }

    private void compileSubroutineDec(){
        subroutineType = Tokenizer.tokenValue();
        symbolTable.startSubroutine();

        writeStructuralTag("subroutineDec", false);
        outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
        Tokenizer.advance();

        if(Tokenizer.tokenValue().equals("void") 
        || Tokenizer.tokenValue().equals("int") 
        || Tokenizer.tokenValue().equals("char") 
        || Tokenizer.tokenValue().equals("boolean")||
        Tokenizer.tokenType().equals("identifier")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            return;
        }

        if(Tokenizer.tokenType().equals("identifier")){
            functionName = Tokenizer.tokenValue();

            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            return;
        }

        if(subroutineType.equals("method")){
            symbolTable.define("this", className, "argument");
        }
        
        if(Tokenizer.tokenValue().equals("(")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
            compileParameterList();
        }else{
            return;
        }

        if(Tokenizer.tokenValue().equals(")")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            return;
        }

        compileSubroutineBody();

        writeStructuralTag("subroutineDec", true);
    }

    private void compileSubroutineBody(){
        writeStructuralTag("subroutineBody", false);

        if(Tokenizer.tokenValue().equals("{")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            return;
        }

        while(Tokenizer.tokenValue().equals("var")){
            compileVardec();
        }

        int numLocals = symbolTable.varCount("var");
        codeWriter.writeSubroutine(subroutineType, className+"."+functionName, numLocals);
        if (subroutineType.equals("constructor")) {
            codeWriter.writePush("constant", symbolTable.varCount("field"));
            vmWriter.println("call Memory.alloc 1");
            codeWriter.writePop("pointer", 0);
        }
        if(subroutineType.equals("method")){
            codeWriter.writePush("argument", 0);
            codeWriter.writePop("pointer", 0);
        }

        compileStatements();

        if(Tokenizer.tokenValue().equals("}")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            return;
        }

        writeStructuralTag("subroutineBody", true);
    }

    private void compileStatements(){
        writeStructuralTag("statements", false);

        while(Tokenizer.tokenValue().equals("let") || 
        Tokenizer.tokenValue().equals("if") || 
        Tokenizer.tokenValue().equals("while") || 
        Tokenizer.tokenValue().equals("do") ||
        Tokenizer.tokenValue().equals("return")){
            if (Tokenizer.tokenValue().equals("let")) {
                compileLetStatement();
            }else if(Tokenizer.tokenValue().equals("if")){
                compileIfStatement();
            }else if(Tokenizer.tokenValue().equals("while")){
                compileWhileStatement();
            }else if(Tokenizer.tokenValue().equals("do")){
                compileDoStatement();
            }else if(Tokenizer.tokenValue().equals("return")){
                compileReturnStatement();
            }
        }

        writeStructuralTag("statements", true);
    }

    private void compileReturnStatement(){
        writeStructuralTag("returnStatement", false);


        outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
        Tokenizer.advance();

        if (Tokenizer.tokenType().equals("integerConstant") ||
            Tokenizer.tokenType().equals("stringConstant") ||
            Tokenizer.tokenType().equals("identifier") ||
            (Tokenizer.tokenType().equals("keyword") &&
             (Tokenizer.tokenValue().equals("true") ||
              Tokenizer.tokenValue().equals("false") ||
              Tokenizer.tokenValue().equals("null") ||
              Tokenizer.tokenValue().equals("this"))) ||
            (Tokenizer.tokenType().equals("symbol") &&
             (Tokenizer.tokenValue().equals("(") ||
              Tokenizer.tokenValue().equals("-") ||
              Tokenizer.tokenValue().equals("~")))) {
            compileExpression();
        }

        codeWriter.writeReturn();

        if(Tokenizer.tokenValue().equals(";")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }

        writeStructuralTag("returnStatement", true);
    }
    //ADD THE XML FUNCTIONALITY
    void compileDoStatement() {
        Tokenizer.advance(); // Consume 'do'
    
        String firstIdentifier = Tokenizer.tokenValue(); // e.g., Output, myObject, draw
        Tokenizer.advance(); // Consume the first identifier
    
        String fullSubroutineName;
        int numArgs = 0; // Counts arguments for the VM 'call' instruction
    
        // Check if it's a 'Class.function()' or 'object.method()' call (has a dot)
        if (Tokenizer.tokenValue().equals(".")) {
            String classNameOrObjectName = firstIdentifier;
            Tokenizer.advance(); // Consume '.'
            String methodName = Tokenizer.tokenValue(); // e.g., printInt, draw
            Tokenizer.advance(); // Consume the method name
    
            // Look up the first identifier in the symbol table to distinguish
            // between 'ClassName.function()' and 'objectName.method()'.
            SymbolEntry entry = symbolTable.lookup(classNameOrObjectName);
    
            if (entry != null && entry.kind != null) { // It's an object.method() call (e.g., myObject.draw())
                // Push the object's base address onto the stack (this becomes argument 0)
                codeWriter.writePush(entry.kind, entry.index);
                numArgs = 1; // Start arg count with 1 for the implicit 'this' pointer
                fullSubroutineName = entry.type + "." + methodName; // Use the object's actual class type
            } else { // It's a static Class.function() call (e.g., Output.printInt())
                fullSubroutineName = classNameOrObjectName + "." + methodName;
                numArgs = 0; // No implicit 'this' pointer for static functions
            }
    
            // Now compile the expression list (arguments for the function/method)
            Tokenizer.advance(); // Consume '('
            numArgs += compileExpressionList(); // Adds the count of explicit arguments
            Tokenizer.advance(); // Consume ')'
    
        } else if (Tokenizer.tokenValue().equals("(")) { // It's a method call on 'this' (e.g., do draw();)
            fullSubroutineName = className + "." + firstIdentifier;
            Tokenizer.advance(); // Consume '('
    
            // Push 'this' (pointer 0) as the first argument
            codeWriter.writePush("pointer", 0);
            numArgs = 1; // Start arg count with 1 for the implicit 'this' pointer
    
            numArgs += compileExpressionList(); // Add the count of explicit arguments
            Tokenizer.advance(); // Consume ')'
    
        } else {
            // This should ideally be caught by the Tokenizer or a stricter parser
            throw new RuntimeException("Syntax error: Expected '(' or '.' after identifier in do statement.");
        }
    
        // Emit the VM 'call' command
        codeWriter.writeCall(fullSubroutineName, numArgs);
    
        // For 'do' statements, the return value (even if non-void) is always ignored.
        // Pop the dummy return value to maintain stack balance.
        codeWriter.writePop("temp", 0);
    
        // Consume the trailing semicolon
        Tokenizer.advance(); // Consume ';'
    }

    private void compileWhileStatement(){
        writeStructuralTag("whileStatement", false);
        String whileLabel = generateLabel("WHILE_");
        String endLabel = generateLabel("WHILE_END_");

        outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
        Tokenizer.advance();

        if(Tokenizer.tokenValue().equals("(")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            System.err.println("Expected a ( after if-statement");
            return;
        }

        codeWriter.writeLabel(whileLabel);

        compileExpression();
        
        codeWriter.writeArithmetic("~");
        codeWriter.writeIfGoto(endLabel);

        if(Tokenizer.tokenValue().equals(")")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            System.err.println("Expected a ) after expression in if statement");
        }

        if (Tokenizer.tokenValue().equals("{")) {
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }
        
        compileStatements();

        codeWriter.writeGoTo(whileLabel);
        codeWriter.writeLabel(endLabel);

        if(Tokenizer.tokenValue().equals("}")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }

        writeStructuralTag("whileStatement", true);
    }

    private String generateLabel(String prefix) {
        return prefix + (labelCounter++);
    }

    private void compileIfStatement(){
        writeStructuralTag("ifStatement", false);

        String labelElse = generateLabel("IF_ELSE_");
        String labelEnd = generateLabel("IF_END_");

        outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
        Tokenizer.advance();

        if(Tokenizer.tokenValue().equals("(")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            System.err.println("Expected a ( after if-statement");
            return;
        }

        compileExpression();

        codeWriter.writeArithmetic("~");
        codeWriter.writeIfGoto(labelElse);

        if(Tokenizer.tokenValue().equals(")")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            System.err.println("Expected a ) after expression in if statement");
        }

        if (Tokenizer.tokenValue().equals("{")) {
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            return;
        }
        
        compileStatements();

        if(Tokenizer.tokenValue().equals("}")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            return;
        }



        //start of the "Else" chain
        if(Tokenizer.tokenValue().equals("else")){
            codeWriter.writeGoTo(labelEnd);
            codeWriter.writeLabel(labelElse);
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
            if (Tokenizer.tokenValue().equals("{")) {
                outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                Tokenizer.advance();
            }else{
                return;
            }
            compileStatements();
            if(Tokenizer.tokenValue().equals("}")){
                outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                Tokenizer.advance();
            }
            codeWriter.writeLabel(labelEnd);
        }else{
            codeWriter.writeLabel(labelElse);
        }

        writeStructuralTag("ifStatement", true);
    }

    private void compileLetStatement(){
        SymbolEntry temp;
        String varName;
        boolean isArrayAssignment = false;

        writeStructuralTag("letStatement", false);

        outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
        Tokenizer.advance();

        if(Tokenizer.tokenType().equals("identifier")){
            varName = Tokenizer.tokenValue();
            temp = symbolTable.lookup(varName);

            if(temp == null){
                System.err.println("Undefined variable name");
            }

            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            System.err.println("Needs an identifier for let statement");
            return;
        }

        if(Tokenizer.tokenValue().equals("[")){
            isArrayAssignment = true;
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();

            codeWriter.writePush(temp.kind, temp.index);

            compileExpression(); //where the base address+the number in here
                                 //get calculated

            codeWriter.writeArithmetic("+");

            if (Tokenizer.tokenValue().equals("]")) {
                outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                Tokenizer.advance();
            }else{
                return;
            }
        }
        
        if(Tokenizer.tokenValue().equals("=")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }

        compileExpression();

        if(isArrayAssignment){
            codeWriter.writePop("temp", 0);
            codeWriter.writePop("pointer", 1);
            codeWriter.writePush("temp", 0);
            codeWriter.writePop("that", 0);
        }else{        
            codeWriter.writePop(temp.kind, temp.index);
        }

        if (Tokenizer.tokenValue().equals(";")) {
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }

        writeStructuralTag("letStatement", true);
    }

    private boolean isBinaryOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/") ||
               token.equals("&") || token.equals("|") || token.equals("<") || token.equals(">") ||
               token.equals("=");
    }

    private void compileExpression(){
        writeStructuralTag("expression", false);

        compileTerm();

        String tempSymbol;

        while (Tokenizer.tokenType().equals("symbol")
        && isBinaryOperator(Tokenizer.tokenValue())) {

            tempSymbol=Tokenizer.tokenValue();

            if (tempSymbol.equals("+") || tempSymbol.equals("-") || tempSymbol.equals("&") ||
            tempSymbol.equals("|") || tempSymbol.equals("<") || tempSymbol.equals(">") || tempSymbol.equals("=")) {

            if (tempSymbol.equals("<")) {
                outputTokenXml("&lt;", Tokenizer.tokenType());
            } else if (tempSymbol.equals(">")) {
                outputTokenXml("&gt;", Tokenizer.tokenType());
            } else if (tempSymbol.equals("&")) {
                outputTokenXml("&amp;", Tokenizer.tokenType());
            } else {
                outputTokenXml(tempSymbol, Tokenizer.tokenType());
            }
            Tokenizer.advance();

            compileTerm();

            switch (tempSymbol) {
                case "+": codeWriter.writeArithmetic(tempSymbol); break;
                case "-": codeWriter.writeArithmetic(tempSymbol); break;
                case "&": codeWriter.writeArithmetic(tempSymbol); break;
                case "|": codeWriter.writeArithmetic(tempSymbol); break;
                case "<": codeWriter.writeArithmetic(tempSymbol); break;
                case ">": codeWriter.writeArithmetic(tempSymbol); break;
                case "=": codeWriter.writeArithmetic(tempSymbol); break;
            }
        } else {
            break;
        }
        }

        writeStructuralTag("expression", true);
    }

    private void compileTerm(){
        writeStructuralTag("term", false);

        if (Tokenizer.tokenType().equals("symbol") && (Tokenizer.tokenValue().equals("-") || Tokenizer.tokenValue().equals("~"))) {
            String unaryOp = Tokenizer.tokenValue();
            outputTokenXml(unaryOp, Tokenizer.tokenType());
            Tokenizer.advance();
            compileTerm();
            if (unaryOp.equals("-")) vmWriter.println("neg");
            else if (unaryOp.equals("~")) codeWriter.writeArithmetic("~");
        } else if (Tokenizer.tokenType().equals("symbol") && Tokenizer.tokenValue().equals("(")) {
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
            compileExpression();
            if (Tokenizer.tokenType().equals("symbol") && Tokenizer.tokenValue().equals(")")) {
                outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                Tokenizer.advance();
            } else {
                System.err.println("Syntax Error: Expected ')' after expression. Found: '" + Tokenizer.tokenValue() + "' (" + Tokenizer.tokenType() + ")");
            }
        } else if (Tokenizer.tokenType().equals("integerConstant")) {
            codeWriter.writePush("constant", Integer.parseInt(Tokenizer.tokenValue()));
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        } else if (Tokenizer.tokenType().equals("stringConstant")) {
            String s = Tokenizer.tokenValue();
            codeWriter.writePush("constant", s.length());
            codeWriter.writeCall("String.new", 1);
            for (char c : s.toCharArray()) {
                codeWriter.writePush("constant", (int) c);
                codeWriter.writeCall("String.appendChar", 2);
            }
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        } else if (Tokenizer.tokenType().equals("keyword") &&
                   (Tokenizer.tokenValue().equals("true") || Tokenizer.tokenValue().equals("false") ||
                    Tokenizer.tokenValue().equals("null") || Tokenizer.tokenValue().equals("this"))) {
            String keyword = Tokenizer.tokenValue();
            switch (keyword) {
                case "true":
                    codeWriter.writePush("constant", 0);
                    codeWriter.writeArithmetic("~");
                    break;
                case "false":
                case "null":
                    codeWriter.writePush("constant", 0);
                    break;
                case "this":
                    codeWriter.writePush("pointer", 0);
                    break;
            }
            outputTokenXml(keyword, Tokenizer.tokenType());
            Tokenizer.advance();
        } else if (Tokenizer.tokenType().equals("identifier")) {
            String name = Tokenizer.tokenValue();
            char nextTokenValue = Tokenizer.lookAhead();

            if (nextTokenValue == '[') {
                outputTokenXml(name, Tokenizer.tokenType());
                Tokenizer.advance();
                outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                Tokenizer.advance();

                SymbolEntry arrayVar = symbolTable.lookup(name);
                if (arrayVar == null) { System.err.println("Error: Undefined array variable '" + name + "'."); }
                else {
                    codeWriter.writePush(arrayVar.kind, arrayVar.index);
                }

                compileExpression();

                codeWriter.writeArithmetic("+");
                codeWriter.writePop("pointer", 1);
                codeWriter.writePush("that", 0);

                if (Tokenizer.tokenType().equals("symbol") && Tokenizer.tokenValue().equals("]")) {
                    outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                    Tokenizer.advance();
                } else {
                    System.err.println("Syntax Error: Expected ']' after array index. Found: '" + Tokenizer.tokenValue() + "' (" + Tokenizer.tokenType() + ")");
                }
            } else if (nextTokenValue == '(' || nextTokenValue == '.') {
                String fullSubroutineName = "";
                int numArgs = 0;

                if (nextTokenValue == '.') {
                    outputTokenXml(name, Tokenizer.tokenType());
                    Tokenizer.advance();
                    outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                    Tokenizer.advance();

                    String methodName = Tokenizer.tokenValue();
                    if (Tokenizer.tokenType().equals("identifier")) {
                        outputTokenXml(methodName, Tokenizer.tokenType());
                        Tokenizer.advance();
                    } else {
                        System.err.println("Syntax Error: Expected method name after '.'. Found: '" + Tokenizer.tokenValue() + "' (" + Tokenizer.tokenType() + ")");
                    }

                    SymbolEntry objEntry = symbolTable.lookup(name);
                    if (objEntry != null && objEntry.kind != null) {
                        codeWriter.writePush(objEntry.kind, objEntry.index);
                        numArgs = 1;
                        fullSubroutineName = objEntry.type + "." + methodName;
                    } else {
                        fullSubroutineName = name + "." + methodName;
                        numArgs = 0;
                    }
                } else {
                    outputTokenXml(name, Tokenizer.tokenType());
                    Tokenizer.advance();
                    fullSubroutineName = className + "." + name;
                    codeWriter.writePush("pointer", 0);
                    numArgs = 1;
                }

                if (Tokenizer.tokenType().equals("symbol") && Tokenizer.tokenValue().equals("(")) {
                    outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                    Tokenizer.advance();
                    numArgs += compileExpressionList();
                } else {
                    System.err.println("Syntax Error: Expected '(' for subroutine call. Found: '" + Tokenizer.tokenValue() + "' (" + Tokenizer.tokenType() + ")");
                }

                if (Tokenizer.tokenType().equals("symbol") && Tokenizer.tokenValue().equals(")")) {
                    outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                    Tokenizer.advance();
                } else {
                    System.err.println("Syntax Error: Expected ')' after expression list. Found: '" + Tokenizer.tokenValue() + "' (" + Tokenizer.tokenType() + ")");
                }
                codeWriter.writeCall(fullSubroutineName, numArgs);
            } else {
                outputTokenXml(name, Tokenizer.tokenType());
                Tokenizer.advance();
                SymbolEntry simpleVar = symbolTable.lookup(name);
                if (simpleVar == null) { System.err.println("Error: Undefined variable '" + name + "'."); }
                else { codeWriter.writePush(simpleVar.kind, simpleVar.index); }
            }
        } else {
            System.err.println("Syntax Error: Unrecognized start of term: '" + Tokenizer.tokenValue() + "' (" + Tokenizer.tokenType() + ")");
        }

        while (Tokenizer.tokenType().equals("symbol") &&
               (Tokenizer.tokenValue().equals("*") || Tokenizer.tokenValue().equals("/"))) {

            String opSymbol = Tokenizer.tokenValue();

            outputTokenXml(opSymbol, Tokenizer.tokenType());
            Tokenizer.advance();

            compileTerm();

            switch (opSymbol) {
                case "*": codeWriter.writeCall("Math.multiply", 2); break;
                case "/": codeWriter.writeCall("Math.divide", 2); break;
            }
        }

        writeStructuralTag("term", true);
    }

    private int compileExpressionList() {
        writeStructuralTag("expressionList", false);
        int counter = 0;

        if (Tokenizer.tokenType().equals("integerConstant") ||
            Tokenizer.tokenType().equals("stringConstant") ||
            Tokenizer.tokenType().equals("identifier") ||
            Tokenizer.tokenType().equals("keyword") &&
                (Tokenizer.tokenValue().equals("true") ||
                 Tokenizer.tokenValue().equals("false") ||
                 Tokenizer.tokenValue().equals("null") ||
                 Tokenizer.tokenValue().equals("this")) ||
            Tokenizer.tokenType().equals("symbol") &&
                (Tokenizer.tokenValue().equals("(") ||
                 Tokenizer.tokenValue().equals("-") ||
                 Tokenizer.tokenValue().equals("~"))) {

            compileExpression();
            counter++;

            while (Tokenizer.tokenType().equals("symbol") && Tokenizer.tokenValue().equals(",")) {
                outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType()); 
                Tokenizer.advance();
                compileExpression();
                counter++;
            }
        }
        

        writeStructuralTag("expressionList", true);
        return counter;
    }

    private void compileVardec(){
        writeStructuralTag("varDec", false);

        String type;
        String name;

        outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
        Tokenizer.advance();
        if(Tokenizer.tokenValue().equals("int") || Tokenizer.tokenValue().equals("char") || Tokenizer.tokenValue().equals("boolean") ||
        Tokenizer.tokenType().equals("identifier")){
            type = Tokenizer.tokenValue();
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();            
        }else{
            return;
        }            
        if(Tokenizer.tokenType().equals("identifier")){
            name = Tokenizer.tokenValue();
            symbolTable.define(name, type, "var");
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();            
        }
        while(Tokenizer.tokenValue().equals(",")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
            if(Tokenizer.tokenType().equals("identifier")){
                name = Tokenizer.tokenValue();
                symbolTable.define(name, type, "var");
                outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                Tokenizer.advance();
            }
        }
        if (Tokenizer.tokenValue().equals(";")) {
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }
        writeStructuralTag("varDec", true);
    }

    private void compileParameterList(){
        String type;
        String name;
        writeStructuralTag("parameterList", false);

        //add a classname to this
        while(Tokenizer.tokenType().equals("identifier") || Tokenizer.tokenValue().equals("int") || Tokenizer.tokenValue().equals("char") || Tokenizer.tokenValue().equals("boolean")){
            type = Tokenizer.tokenValue();
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();

            if(Tokenizer.tokenType().equals("identifier")){
                name = Tokenizer.tokenValue();
                symbolTable.define(name, type, "argument");
                outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                Tokenizer.advance();
            }else{
                return;
            }
            while(Tokenizer.tokenValue().equals(",")){
                outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                Tokenizer.advance();

                //add a classname to this
                if(Tokenizer.tokenValue().equals("int") || Tokenizer.tokenValue().equals("char") || Tokenizer.tokenValue().equals("boolean")){
                    type = Tokenizer.tokenValue();
                    outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                    Tokenizer.advance();
                }else{
                    return;
                }
                if(Tokenizer.tokenType().equals("identifier")){
                    name = Tokenizer.tokenValue();
                    symbolTable.define(name, type, "argument");
                    outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                    Tokenizer.advance();
                }else{
                    return;
                }
            }
        }
        writeStructuralTag("parameterList", true);
    }

    private void compileClassVarDec(){
        String name;
        String type;
        String kind;

        writeStructuralTag("classVarDec", false);

        kind = Tokenizer.tokenValue();

        outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
        Tokenizer.advance();

        if(Tokenizer.tokenValue().equals("int") || Tokenizer.tokenValue().equals("char") || Tokenizer.tokenValue().equals("boolean") ||
        Tokenizer.tokenType().equals("identifier")){
            type = Tokenizer.tokenValue();
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            System.err.println("Syntax Error: Expected type after static/type.");
            return;
        }

        if(Tokenizer.tokenType().equals("identifier")){
            name = Tokenizer.tokenValue();
            symbolTable.define(name, type, kind);
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            System.err.println("Syntax Error: Expected identifier after variable type.");
            return;
        }

        while(Tokenizer.tokenType().equals("symbol") && Tokenizer.tokenValue().equals(",")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
            if (Tokenizer.tokenType().equals("identifier")) {
                name = Tokenizer.tokenValue();
                symbolTable.define(name, type, kind);
                outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
                Tokenizer.advance();
            }else{
                return;
            }
        }

        if(Tokenizer.tokenType().equals("symbol") && Tokenizer.tokenValue().equals(";")){
            outputTokenXml(Tokenizer.tokenValue(), Tokenizer.tokenType());
            Tokenizer.advance();
        }else{
            return;
        }

        writeStructuralTag("classVarDec", true);
    }

    private void writeStructuralTag(String tagName, boolean isClosingTag) {
        if (isClosingTag) {
            indent--;
            for (int i = 0; i < indent; i++) {
                writer.print("  ");
            }
            writer.println("</" + tagName + ">");
        } else {
            for (int i = 0; i < indent; i++) {
                writer.print("  ");
            }
            writer.println("<" + tagName + ">");
            indent++;
        }
    }
}
