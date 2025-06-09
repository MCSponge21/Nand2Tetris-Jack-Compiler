import java.util.HashMap;

public class SymbolTable {
    private HashMap<String, SymbolEntry> classTable;
    private HashMap<String, SymbolEntry> subroutineTable;

    private int staticCount;
    private int fieldCount;
    private int argCount;
    private int varCount;

    public SymbolTable(){
        classTable = new HashMap<>();
        subroutineTable = new HashMap<>();
    }

    public void startSubroutine(){
        subroutineTable.clear();
        varCount = 0;
        argCount = 0;
    }

    public void resetAllCounts(){
        staticCount = 0;
        fieldCount = 0;
        argCount = 0;
        varCount = 0;
    }

    public void define(String name, String type, String kind){
        int index;
        if(kind.equals("static")){
            index = staticCount;
            classTable.put(name, new SymbolEntry(type, kind, index));
            staticCount++;
        }else if(kind.equals("field")){
            index = fieldCount;
            classTable.put(name, new SymbolEntry(type, kind, index));
            fieldCount++;
        }else if(kind.equals("argument")){
            index = argCount;
            classTable.put(name, new SymbolEntry(type, kind, index));
            argCount++;
        }else if(kind.equals("var")){
            index = varCount;
            classTable.put(name, new SymbolEntry(type, kind, index));
            varCount++;
        }else{
            throw new IllegalArgumentException("Unknown variable kind: "+kind);
        }
    }

    public SymbolEntry lookup(String name){
        if(subroutineTable.containsKey(name)){
            return subroutineTable.get(name);
        }

        if (classTable.containsKey(name)) {
            return classTable.get(name);
        }

        return null;
    }

    public int varCount(String kind) {
        switch (kind) {
            case "static": return staticCount;
            case "field": return fieldCount;
            case "argument": return argCount;
            case "var": return varCount;
            default: return 0;
        }
    }
}
