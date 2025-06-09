import java.io.*;

public class CodeWriter {
    private PrintWriter writer;

    public CodeWriter(PrintWriter writer){
        this.writer = writer;
    }

    public void writePush(String kind, int index){
        if(kind.equals("field")){kind = "this";}
        if(kind.equals("var")){kind = "local";}
        writer.println("push "+kind+" "+index);
    }

    public void writePop(String kind, int index){
        if(kind.equals("field")){kind = "this";}
        if(kind.equals("var")){kind = "local";}
        writer.println("pop "+kind+" "+index);
    }

    public void writeSubroutine(String type, String name, int numLocals){
        if(type.equals("method")){
            writer.println("function "+name+" "+numLocals);
        }else if(type.equals("constructor")){
            
            writer.println("function "+name+" "+numLocals);
        }else{
            writer.println("function "+name+" "+numLocals);
        }
    }

    public void writeReturn(){
        writer.println("return");
    }

    public void writeCall(String kind, int index){
        writer.println("call "+kind+" "+index);
    }

    public void writeIfGoto(String label) {
        writer.println("if-goto " + label);
    }

    public void writeLabel(String label) {
        writer.println("label " + label);
    }

    public void writeGoTo(String label) {
        writer.println("goto " + label);
    }

    public void writeArithmetic(String symbol){
        if(symbol.equals("+")){
            writer.println("add");
        }else if(symbol.equals("-")){
            writer.println("sub");
        }else if(symbol.equals(">")){
            writer.println("gt");
        }else if(symbol.equals("<")){
            writer.println("lt");
        }else if(symbol.equals("&")){
            writer.println("and");
        }else if(symbol.equals("|")){
            writer.println("or");
        }else if(symbol.equals("~")){
            writer.println("not");
        }else if(symbol.equals("=")){
            writer.println("eq");
        }
    }
}
