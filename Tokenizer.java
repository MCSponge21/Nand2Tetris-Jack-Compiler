import java.util.*;

public class Tokenizer{

    private static HashMap<Character, String> symbols = new HashMap<>();
    private static HashMap<String, String> keywords = new HashMap<>();

    private static int currentIndex = 0;
    private static String currentToken = null;
    private static String currentType = null;
    private static String currentFileContent = null;

    static{
        symbols.put('{', "Symbol");
        symbols.put('}', "Symbol");
        symbols.put('[', "Symbol");
        symbols.put(']', "Symbol");
        symbols.put(')', "Symbol");
        symbols.put('(', "Symbol");
        symbols.put('.', "Symbol");
        symbols.put(',', "Symbol");
        symbols.put(';', "Symbol");
        symbols.put('+', "Symbol");
        symbols.put('-', "Symbol");
        symbols.put('~', "Symbol");
        symbols.put('*', "Symbol");
        symbols.put('/', "Symbol");
        symbols.put('&', "Symbol");
        symbols.put('|', "Symbol");
        symbols.put('<', "Symbol");
        symbols.put('>', "Symbol");
        symbols.put('=', "Symbol");
        keywords.put("class", "Keyword");
        keywords.put("method", "Keyword");
        keywords.put("function", "Keyword");
        keywords.put("constructor", "Keyword");
        keywords.put("int", "Keyword");
        keywords.put("boolean", "Keyword");
        keywords.put("char", "Keyword");
        keywords.put("void", "Keyword");
        keywords.put("var", "Keyword");
        keywords.put("static", "Keyword");
        keywords.put("field", "Keyword");
        keywords.put("let", "Keyword");
        keywords.put("do", "Keyword");
        keywords.put("if", "Keyword");
        keywords.put("else", "Keyword");
        keywords.put("while", "Keyword");
        keywords.put("return", "Keyword");
        keywords.put("true", "Keyword");
        keywords.put("false", "Keyword");
        keywords.put("null", "Keyword");
        keywords.put("this", "Keyword");
    }

    public static void initialize(String fileContent){
        currentFileContent = fileContent;
        currentIndex = 0;
        currentToken = null;
        currentType = null;
    }

    public static String getTokenType(String token){
        if(token.length() == 1 && symbols.containsKey(token.charAt(0))){
            return "symbol";
        }else if(keywords.containsKey(token)){
            return "keyword";
        }else if(token.charAt(0)=='"' && token.charAt(token.length()-1)=='"'){
            return "stringConstant";
        }else{
            try{
                int intValue = Integer.parseInt(token);
                return "integerConstant";
            }catch(NumberFormatException e){
                return "identifier";
            }
        }
    }

    public static void advance(){
        while (currentIndex < currentFileContent.length()) {
            char c = currentFileContent.charAt(currentIndex);

            if (Character.isWhitespace(c)) {
                currentIndex++; 
            } else if (c == '/' && currentIndex + 1 < currentFileContent.length()) {
                char nextC = currentFileContent.charAt(currentIndex + 1);
                if (nextC == '/') { 
                    currentIndex += 2;
                    while (currentIndex < currentFileContent.length() &&
                           currentFileContent.charAt(currentIndex) != '\n' &&
                           currentFileContent.charAt(currentIndex) != '\r') {
                        currentIndex++;
                    }
                    if (currentIndex < currentFileContent.length() && currentFileContent.charAt(currentIndex) == '\r') {
                        currentIndex++;
                    }
                    if (currentIndex < currentFileContent.length() && currentFileContent.charAt(currentIndex) == '\n') {
                        currentIndex++;
                    }
                } else if (nextC == '*') {
                    currentIndex += 2; 
                    boolean foundEnd = false;
                    while (currentIndex + 1 < currentFileContent.length() && !foundEnd) {
                        if (currentFileContent.charAt(currentIndex) == '*' && currentFileContent.charAt(currentIndex + 1) == '/') {
                            currentIndex += 2; 
                            foundEnd = true;
                        } else {
                            currentIndex++;
                        }
                    }
                    if (!foundEnd) {
                        System.err.println("Error: Unclosed multi-line comment starting at index " + (currentIndex - 2));
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        String foundToken = "";

        if (currentIndex >= currentFileContent.length()) {
            currentToken = null;
            currentType = null;
            return;
        }

        char currentChar = currentFileContent.charAt(currentIndex);

        if(symbols.containsKey(currentChar)){
            foundToken += currentChar;
            currentIndex++;
        }else if(currentChar == '"'){
            foundToken += currentChar; 
            currentIndex++;
            while(currentIndex < currentFileContent.length()){
                char innerChar = currentFileContent.charAt(currentIndex);
                if(innerChar == '"'){
                    foundToken += innerChar; 
                    currentIndex++;
                    break; 
                }
                foundToken += innerChar;
                currentIndex++;
            }
            if (!foundToken.endsWith("\"")) {
                System.err.println("Error: Unterminated string literal starting at index " + (currentIndex - foundToken.length()) + " in currentFileContent.");
            }
        }else { 
            while(currentIndex < currentFileContent.length()){
                char tempChar = currentFileContent.charAt(currentIndex);
                if(Character.isWhitespace(tempChar) || symbols.containsKey(tempChar) ||
                   (tempChar == '/' && currentIndex + 1 < currentFileContent.length() && (currentFileContent.charAt(currentIndex + 1) == '/' || currentFileContent.charAt(currentIndex + 1) == '*'))){
                    break;
                }
                foundToken += tempChar;
                currentIndex++;
            }
        }

        currentToken = foundToken;
        currentType = getTokenType(foundToken);
        if(currentType.equals("stringConstant")){
            String newToken = "";
            for(int i = 0; i < currentToken.length(); i++){
                if(currentToken.charAt(i) != '"'){
                    newToken+=currentToken.charAt(i);
                }
            }
            System.out.println(newToken);
            currentToken = newToken;
        }
    }

    public static Boolean hasMoreTokens(){
        int index = currentIndex;
        while(index < currentFileContent.length()){
            if(Character.isWhitespace(currentFileContent.charAt(index))){
                index++;
            }else{
                currentIndex = index;

                return true;
            }
        }
        return false;
    }

    public static char lookAhead(){
        return currentFileContent.charAt(currentIndex);
    }

    public static String tokenType() {
        return currentType;
    }

    public static String tokenValue() {
        return currentToken;
    }

    
}