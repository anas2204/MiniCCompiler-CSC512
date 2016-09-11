import java.io.*;
import java.util.ArrayList;

public class Scanner {

    BufferedReader inputReader = null;
    private String currentLine = null;

    //CONSTANTS defined by the C Grammar
    private ArrayList<String> ReservedList;
    private ArrayList<String> RelOpList;
    private ArrayList<String> LogOpList;
    private ArrayList<String> MathOpList;
    private ArrayList<String> SymbolList;

    Scanner(String fileName)
    {
        populateLists();

        try {
            inputReader = new BufferedReader (new FileReader(fileName));
            currentLine = inputReader.readLine();
            shiftOrRead(0);
        }
        catch (FileNotFoundException e) {
            System.out.println("Cannot Open File!");
        } catch (IOException e) {
            System.out.println("Cannot Read Line");
        }
    }

    //Populating the Reserved Words, Symbols and Operators in Highest to Lowest length --> Maximal greedy matching
    void populateLists()
    {
        ReservedList = new ArrayList<>();
        ReservedList.add("continue");
        ReservedList.add("decimal");
        ReservedList.add("return");
        ReservedList.add("binary");
        ReservedList.add("while");
        ReservedList.add("print");
        ReservedList.add("write");
        ReservedList.add("break");
        ReservedList.add("void");
        ReservedList.add("read");
        ReservedList.add("int");
        ReservedList.add("if");

        RelOpList = new ArrayList<>();
        RelOpList.add("==");
        RelOpList.add("<=");
        RelOpList.add(">=");
        RelOpList.add("!=");
        RelOpList.add("<");
        RelOpList.add(">");

        LogOpList = new ArrayList<>();
        LogOpList.add("&&");
        LogOpList.add("||");
        LogOpList.add("!");

        MathOpList = new ArrayList<>();
        MathOpList.add("+=");
        MathOpList.add("-=");
        MathOpList.add("*=");
        MathOpList.add("/=");
        MathOpList.add("+");
        MathOpList.add("-");
        MathOpList.add("*");
        MathOpList.add("/");

        SymbolList = new ArrayList<>();
        SymbolList.add("(");
        SymbolList.add(")");
        SymbolList.add("{");
        SymbolList.add("}");
        SymbolList.add("[");
        SymbolList.add("]");
        SymbolList.add(",");
        SymbolList.add(";");
        SymbolList.add("=");
    }

    //Either Shifts currentLine by shiftLength, or reads a new line if there is nothing left
    void shiftOrRead (int shiftLength)
    {
        try {
            currentLine = currentLine.substring(shiftLength);
            currentLine = currentLine.trim();

            //All tokens in currentLine are used --> Get new line and return. While loop because readLine() reads the "\n" at the end of line
            while (currentLine.isEmpty() || currentLine.length() == 0)
            {
                currentLine = inputReader.readLine();

                if(currentLine != null)
                    currentLine = currentLine.trim();

                else                                        //EOF reached
                    break;
            }
        }
        catch (IOException e) {
            System.out.println("Exception in ShiftOrRead");
        }
    }

    //Returns a new Token along with removing current token from currentLine
    Token newToken (TokenType type, String name)
    {
        shiftOrRead(name.length());

        return new Token(type, name);
    }

    //Deterimines if file has more tokens left. If not, close the inputReader
    public boolean hasMoreTokens()
    {
        if (currentLine != null)
            return true;

        else
        {
            try {
                inputReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /*
    Returns a new Token.
    currentLine is always populated to read the token.
    */
    public Token getNextToken()
    {
        //Check Meta lines. Assumption --> Meta lines do not exceed 1 line and span the entire line
        if (currentLine.startsWith("#") || currentLine.startsWith("//"))
            return newToken(TokenType.META, currentLine);

        //Check Reserved Keywords
        for (String reservedWord : ReservedList)
        {
            char ch;
            //To check if keyword isn't an ID instead --> Eg: int123, _char50
            if (currentLine.startsWith(reservedWord) && !(Character.isLetterOrDigit(ch = currentLine.charAt(reservedWord.length())) || ch == '_'))
                return newToken(TokenType.RESERVED, reservedWord);
        }

        //Check Relational Operators
        for (String relOp : RelOpList)
        {
            if (currentLine.startsWith(relOp))
                return newToken(TokenType.RELOP, relOp);
        }

        //Check Logical Operators
        for (String logOp : LogOpList)
        {
            if (currentLine.startsWith(logOp))
                return newToken(TokenType.LOGOP, logOp);
        }

        //Check Math Operators
        for (String mathOp : MathOpList)
        {
            if (currentLine.startsWith(mathOp))
                return newToken(TokenType.MATHOP, mathOp);
        }

        //Check Special Symbols
        for (String symbols : SymbolList)
        {
            if (currentLine.startsWith(symbols))
                return newToken(TokenType.SYMBOL, symbols);
        }

        //Check String Literals. Assumption --> String literals do not stretch beyond 1 line.
        if (currentLine.startsWith("\""))
        {
            int pos = currentLine.indexOf("\"", 1);

            if (pos != -1)
            {
                return newToken(TokenType.STRING, currentLine.substring(0, pos+1));
            }
        }

        //Check Numeric Literals
        if (Character.isDigit(currentLine.charAt(0)))
        {
            char ch;    int i=1;
            String tempToken = currentLine.charAt(0)+"";

            while (i < currentLine.length() && Character.isDigit(ch = currentLine.charAt(i)))
            {
                tempToken += ch;    i++;
            }
            return newToken(TokenType.NUMBER, tempToken);
        }

        //Check IDs
        if(Character.isLetter(currentLine.charAt(0)) || currentLine.charAt(0) == '_')
        {
            char ch; int i=1;
            String tempToken = currentLine.charAt(0)+"";

            while (i < currentLine.length() && (Character.isLetterOrDigit(ch = currentLine.charAt(i)) || ch == '_'))
            {
                tempToken += ch;    i++;
            }
            return newToken(TokenType.IDENTIFIER, tempToken);
        }

        return newToken(TokenType.UNDEFINED, currentLine);
    }

    public static void main(String[] args) throws IOException
    {
        if (args.length != 1)
        {
            System.out.println ("INCORRECT Execution of program. Enter as: \"Scanner <fileName>\"");
            System.exit(0);
        }

        String fileName = args[0];
        Scanner s = new Scanner(fileName);

        BufferedWriter outputWriter = new BufferedWriter(new FileWriter(fileName.substring(0,fileName.length()-2)+"_gen.c"));

        while (s.hasMoreTokens())
        {
            Token token = s.getNextToken();

            if (token.getTokenType() == TokenType.UNDEFINED)
            {
                System.out.println("Program is INCORRECT! Terminating Conversion");
                break;
            }

            switch (token.getTokenType())
            {
                case IDENTIFIER: if (!token.getTokenName().equals("main"))
                                    outputWriter.write("csc512"+token.getTokenName());
                                else
                                    outputWriter.write(token.getTokenName());
                                break;

                case META:      outputWriter.write(token.getTokenName());
                                outputWriter.write("\n");
                                break;
                case RELOP:
                case NUMBER:
                case LOGOP:
                case MATHOP:
                case STRING:
                case SYMBOL:    outputWriter.write(token.getTokenName());
                                if (token.getTokenName().equals("{") || token.getTokenName().equals("}") || token.getTokenName().equals(";"))
                                    outputWriter.write("\n");
                                break;

                case RESERVED:  outputWriter.write(token.getTokenName());
                                outputWriter.write(" ");
                                break;
            }
        }
        outputWriter.close();
    }
}