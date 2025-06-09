# Nand2Tetris Jack Compiler

This project is a custom implementation of a **Jack Compiler** built for the **Nand2Tetris course**. Its primary function is to translate **Jack code** (a high-level, object-oriented language) into **XML** for structured representation and **VM code** for execution on the **Hack virtual machine**. This compiler is a crucial component in building a complete computer system from first principles.

---

## Features

* **Tokenizer**: Breaks Jack source code down into individual **tokens** for efficient parsing.
* **Parser**: Converts the stream of tokens into a structured **XML representation** and generates executable **VM code**.
* **CodeWriter**: Responsible for producing the low-level **VM instructions** for arithmetic operations, memory access, and control flow.
* **SymbolTable**: Manages **variable** and **subroutine scope**, ensuring correct referencing throughout the compilation process.
* **Error Handling**: Robustly detects and reports various compilation issues, including syntax errors, unclosed comments, and unterminated string literals.

---

## Project Structure

### Files

* `Tokenizer.java`: Handles the **tokenization** of Jack source code, including comments, string literals, and symbols.
* `Parser.java`: Orchestrates the **parsing** of tokens into XML and VM code, compiling classes, subroutines, statements, and expressions.
* `CodeWriter.java`: Focuses solely on **generating VM code** based on the parser's instructions.
* `SymbolTable.java`: Manages the **scope** and **type information** for all declared identifiers.

### Key Methods

#### `Tokenizer`

* `initialize(String fileContent)`: Loads the Jack source code for processing.
* `advance()`: Moves to and processes the next token.
* `getTokenType(String token)`: Determines the **category** (e.g., keyword, symbol, identifier) of a given token.
* `hasMoreTokens()`: Checks if there are more tokens left to process in the input.

#### `Parser`

* `compileClass()`: The entry point for parsing a complete Jack class.
* `compileSubroutineDec()`: Handles the compilation of **functions**, **methods**, and **constructors**.
* `compileStatements()`: Processes the various types of statements in Jack (e.g., `let`, `if`, `while`, `do`, `return`).
* `compileExpression()`: Parses **expressions** involving operands and operators.

#### `CodeWriter`

* `writeArithmetic(String command)`: Generates VM code for arithmetic and logical operations (e.g., `add`, `sub`, `neg`, `not`).
* `writePush(String segment, int index)`: Generates VM code to **push** a value from a specified memory segment onto the stack.
* `writePop(String segment, int index)`: Generates VM code to **pop** a value from the stack into a specified memory segment.

---

## How to Use

### Compilation

To compile and run the Jack Compiler:

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/your-username/your-repo-name.git](https://github.com/your-username/your-repo-name.git)
    cd your-repo-name
    ```
    (Replace `your-username/your-repo-name` with your actual GitHub path.)
2.  **Compile the Java files:**
    ```bash
    javac *.java
    ```
3.  **Run the compiler:**
    ```bash
    java JackCompiler <input_file_or_directory.jack>
    ```

### Input

Provide a `.jack` file or a directory containing `.jack` files as input.

**Example Jack Code:**
```jack
// Main.jack
class Main {
    function void main() {
        var int x, y, sum;
        let x = 10;
        let y = 20;
        let sum = x + y;
        do Output.printInt(sum);
        return;
    }
}
