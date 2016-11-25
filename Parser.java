

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class Parser {

    //Scanner and token
    private Scanner scanner;
    Token currentToken = null;
    Token prevToken = null;

    //File Containing Meta statements
    BufferedWriter outputWriter;

    //Program Counters
    private int variableCounter;
    private int functionCounter;
    private int statementCounter;

    //Symbol Tables
    private HashMap<String, Integer> globalTable=null;
    private HashMap<String, Integer> localTable=null;

    //Variable Counters (For Intermediate Representation)
    private int globalVariableCounter;
    private int localVariableCounter;

    //Buffer holding function to write
    StringBuilder functionBuffer;

    //Boolean to get local/Global scope
    private boolean isLocal;                 //Whether the IDs getting declared are local to a function or global
    private boolean declareVariables;        //Tells us whether we are declaring IDs OR using IDs in expressions
    private boolean appendToBuffer;         //Tells if we should append to buffer or NOT (during Data declaration)
    private boolean inParameterList;        //Tells whether the current context is a Parameter list of a function

    //Global counter for Labels
    private int labelCounter;

    //Stacks for labels and Loops => To keep track of nested IFs and WHILEs
    private Stack<Branch> labelStack;
    private Stack<Branch> whileStack;

    //Variables for 1st Global variable
    private String firstGlobal;
    private boolean globalVariablesFinished;

    Parser(String inputFileName)
    {
        scanner = new Scanner(inputFileName);

        try {
            outputWriter = new BufferedWriter(new FileWriter(inputFileName.replaceFirst(".c","_gen.c")));
        }
        catch (Exception e)
        {
            System.out.println("Exception in Parser Constructor");
        }

        //Init Symbol Tables
        globalTable = new HashMap<String, Integer>();
        localTable = new HashMap<String, Integer>();

        //Initializing Counters to 0
        variableCounter = functionCounter = statementCounter = 0;
        globalVariableCounter = localVariableCounter = 0;
        labelCounter = 0;

        //Boolean assignments
        isLocal = false;
        declareVariables = false;
        appendToBuffer = true;
        inParameterList = false;

        //Buffer
        functionBuffer = new StringBuilder("");

        //Init Stack
        labelStack = new Stack<>();
        whileStack = new Stack<>();

        //1st Global
        firstGlobal = "";
        globalVariablesFinished = false;

        //Get the 1st Token Before Parsing starts
        nextToken();
    }

    public int getVariableCounter()
    {
        return variableCounter;
    }

    public int getFunctionCounter()
    {
        return functionCounter;
    }

    public int getStatementCounter()
    {
        return statementCounter;
    }

    //Gets the Next token from the Scanner. When Tokens are finished, returns an EOF Token and closes output file
    private void nextToken()
    {
        prevToken = currentToken;
        //CHECK IF CURRENT TOKEN IS NOT ID or META. AppendToBuffer is to check for Data declarations or IF/WHILE braces
        if (appendToBuffer && currentToken!= null && currentToken.getTokenType() != TokenType.META && currentToken.getTokenType() != TokenType.IDENTIFIER)
            functionBuffer.append(currentToken.getTokenName()+" ");

        if(appendToBuffer && currentToken!= null &&
                (currentToken.getTokenName().equals("{") ||
                 currentToken.getTokenName().equals("}")  ||
                 currentToken.getTokenName().equals(";")
                ))
        {
            functionBuffer.append("\n");
        }

        try
        {
            if (currentToken!= null && (currentToken.getTokenType() == TokenType.EOF || currentToken.getTokenType() == TokenType.UNDEFINED))
                return;

            if (scanner.hasMoreTokens())
            {
                currentToken = scanner.getNextToken();

                //System.out.println("New Token:"+currentToken.getTokenName());

                if (currentToken.getTokenType() == TokenType.META)
                {
                    outputWriter.write(currentToken.getTokenName()+"\n");
                    nextToken();
                }
            }
            else                                                //All tokens have been exhausted -> Reached EOF
            {
                //System.out.println("All tokens Exhausted");
                currentToken = new Token(TokenType.EOF,"");
            }
        }
        catch (Exception e)
        {
            System.out.println("Exception in nextToken():"+e.getMessage());
        }
    }

    //Matches Current Token with "str" and gets next Token if match was successful
    private boolean matchToken (String str)
    {
        if (currentToken.getTokenType() == TokenType.EOF || currentToken.getTokenType() == TokenType.UNDEFINED)     //Do not match with these tokens
            return false;

        if (currentToken.getTokenName().equals(str))        //Matching with a valid token
        {
            nextToken();                                    //Get next token after current is matched
            return true;
        }
        return false;                                       //Could not match with a valid token
    }

    //Check if Current token is a Number
    private boolean isNumber()
    {
        boolean originalState = appendToBuffer;

        appendToBuffer = false;
        if (currentToken.getTokenType() == TokenType.NUMBER)
        {

            nextToken();
            appendToBuffer = originalState;
            return true;
        }
        appendToBuffer = originalState;
        return false;
    }

    //Check if Current token is a String
    private boolean isString()
    {
        if (currentToken.getTokenType() == TokenType.STRING)
        {
            nextToken();
            return true;
        }
        return false;
    }

    //Check if Current token is an ID
    private boolean isIdentifier()
    {
        if (currentToken.getTokenType() == TokenType.IDENTIFIER)
        {
            nextToken();
            return true;
        }
        else
            return false;
    }

    //Following 3 functions taken from -> www.geeksforgeeks.org/expression-evaluation/
    public String evaluate(ArrayList<String> expression)
    {
        Stack<String> Values = new Stack<String>(), Operations = new Stack<String>();

        for (int i = 0; i < expression.size(); i++)
        {
            if (expression.get(i).equals("("))
                Operations.push(expression.get(i));

            else if (expression.get(i).equals(")"))
            {
                while (Operations.peek().equals("("))
                    Values.push(apply(Operations.pop(), Values.pop(), Values.pop()));
                Operations.pop();
            }

            else if (expression.get(i).equals("+") || expression.get(i).equals("-") || expression.get(i).equals("*") || expression.get(i).equals("/"))
            {
                while (!Operations.empty() && precedence(expression.get(i), Operations.peek()))
                    Values.push(apply(Operations.pop(), Values.pop(), Values.pop()));
                Operations.push(expression.get(i));
            }

            else
            {
                Values.push(expression.get(i));
            }
        }
        while (!Operations.empty())
            Values.push(apply(Operations.pop(), Values.pop(), Values.pop()));

        System.out.println("Evaluated:"+Values.peek());

        return Values.pop();
    }

    public boolean precedence(String o1, String o2)
    {
        if (o2.equals("(") || o2.equals(")"))
            return false;
        if ((o1.equals("*") || o1.equals("/")) && (o2.equals("+") || o2.equals("-")))
            return false;
        else
            return true;
    }

    public String apply(String o, String b, String a)
    {
        switch (o)
        {
            case "+":
                functionBuffer.append("local[" + localVariableCounter + "] = " + a + " + " + b +";\n");
                return "local[" + localVariableCounter++ + "]";
            case "-":
                functionBuffer.append("local[" + localVariableCounter + "] = " + a + " - " + b +";\n");
                return "local[" + localVariableCounter++ + "]";
            case "*":
                functionBuffer.append("local[" + localVariableCounter + "] = " + a + " * " + b +";\n");
                return "local[" + localVariableCounter++ + "]";
            case "/":
                functionBuffer.append("local[" + localVariableCounter + "] = " + a + " / " + b +";\n");
                return "local[" + localVariableCounter++ + "]";
        }
        return null;
    }

    //Puts the passed String in the Local Map of the function currently in context of scanning
    private void putInLocalMap(String IDName)
    {
        //System.out.println("Local Map:"+IDName);
        localTable.put(IDName, localVariableCounter);
        localVariableCounter++;
    }

    //Puts the passed String in the Global Map
    private void putInGlobalMap(String IDName)
    {
        System.out.println("Global:"+IDName);
        globalTable.put(IDName, globalVariableCounter);
        globalVariableCounter++;
    }

    //Replaces the ID with "global" or "local" based on current scope
    private String getStringFromMap (String ID)
    {
        if (localTable.containsKey(ID))
            return "local["+localTable.get(ID)+"]";

        else if (globalTable.containsKey(ID))
            return "global["+globalTable.get(ID)+"]";

        else
            return ID;
    }

    //Clears Local HashMap data
    private void clearLocalData()
    {
        //DEBUG
        if (localTable != null)
        {
            //System.out.println("Printing Local Hashmap");
            for (String key: localTable.keySet())
            {
                System.out.println(key+":"+localTable.get(key));
            }
        }

        //Change $ to real size of local[]
        String temp = functionBuffer.toString().replace("$",localTable.size()+"");
        functionBuffer = new StringBuilder(temp);

        try {
            outputWriter.write(functionBuffer+"\n");
            functionBuffer = new StringBuilder("");
        }
        catch (IOException e) {
            System.out.println("Error writing to File in clearLocalData");
        }

        localTable = new HashMap<String, Integer>();
        localVariableCounter = 0;

        //Since exiting a function
        isLocal = false;
    }

    //This function generates the local[] at the start of the function definition using parameters
    private void makeCodeFromParameters()
    {
        String code = "int local[$];\n";

        if(localTable != null)
        {
            for (String key: localTable.keySet())
            {
                code += "local["+localTable.get(key)+"] = "+key+";\n";
            }
        }

        //Write the generated code
        functionBuffer.append(code);
    }

    //Creates int global[] after 1st function is encountered
    private void makeCodeFromGlobalMap()
    {
        //System.out.println("Creating global[]!");

        //Create global[] and write w/o buffering (Since buffer already contains some function)
        if(globalTable != null)
        {
            int size = globalTable.size();

            if (size > 0)
            {
                try {
                    outputWriter.write("int global["+size+"];\n");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error writing in global[]");
                }
            }
        }

        //No more Global variables
        globalVariablesFinished = true;
    }

    private void makeBranchAndStackItUp(boolean isIf)       //isIf = true implies IF, else WHILE
    {
        Branch newBranch;

        if (isIf)
            newBranch = new Branch("c"+labelCounter++, "c"+labelCounter++, null);

        else
            newBranch = new Branch("c"+labelCounter++, "c"+labelCounter++, "c"+labelCounter++);

        labelStack.push(newBranch);

        //If it is WHILE, push in whileStack too => For "continue" and "break"
        if (!isIf)
            whileStack.push(newBranch);
    }

    private void makeLocalVariableAndWriteToBuffer(String rightHand)
    {
        functionBuffer.append("local["+localVariableCounter++ + "] = " + rightHand+";\n");
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~GRAMMAR STARTS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private boolean typeName()
    {
        return (matchToken("int") || matchToken("void") || matchToken("binary") || matchToken("decimal"));
    }

    private boolean conditionOperator()
    {
        if (currentToken.getTokenType() == TokenType.LOGOP)
        {
            if (matchToken("&&") || matchToken("||"))
                return true;
        }
        return false;
    }

    private boolean comparisonOperator()
    {
        if (currentToken.getTokenType() == TokenType.RELOP)
        {
            nextToken();
            return true;
        }
        return false;
    }

    private boolean breakStatement()
    {
        appendToBuffer = false;
        if (matchToken("break") && matchToken(";"))
        {
            functionBuffer.append("goto "+whileStack.peek().exit+";\n");
            return true;
        }
        appendToBuffer = true;
        return false;
    }

    private boolean continueStatement()
    {
        appendToBuffer = false;
        if (matchToken("continue") && matchToken(";"))
        {
            functionBuffer.append("goto "+whileStack.peek().reEntry+";\n");
            return true;
        }
        appendToBuffer = true;
        return false;
    }

    private boolean addOp(ArrayList<String> expression)
    {
        appendToBuffer = false;

        if (!(matchToken("+") || matchToken("-")))
        {
            appendToBuffer = true;
            return false;
        }

        appendToBuffer = true;
        expression.add(prevToken.getTokenName());
        return true;
    }

    private boolean mulOp(ArrayList<String> expression)
    {
        appendToBuffer = false;

        if (!(matchToken("*") || matchToken("/")))
        {
            appendToBuffer = true;
            return false;
        }

        appendToBuffer = true;
        expression.add(prevToken.getTokenName());
        return true;
    }

    private boolean term_prime(ArrayList<String> expression)
    {
        if (mulOp(expression))
        {
            return (factor(expression) && term_prime(expression));
        }

        return true;
    }

    private boolean term(ArrayList<String> expression)
    {
        return (factor(expression) && term_prime(expression));
    }

    private boolean factor_prime()
    {
        ArrayList<String> expression = new ArrayList<>();

        if (matchToken("["))
            return (expression(expression) && matchToken("]"));

        else  if (matchToken("("))
            return (expression_list() && matchToken(")"));

        else
            return true;
    }

    //TODO
    private boolean factor(ArrayList<String> expression)
    {
        boolean originalState = appendToBuffer;

        appendToBuffer = false;

        if (matchToken("("))
        {
            ArrayList<String> tempExpression = new ArrayList<>();

            if (!expression(tempExpression))
            {
                return false;
            }

            if (tempExpression.size() > 0)
                expression.add(evaluate(tempExpression));

            appendToBuffer = false;

            if (matchToken(")"))
            {
                appendToBuffer = originalState;
                return true;
            }
        }

        else if (matchToken("-") && isNumber())
        {
            makeLocalVariableAndWriteToBuffer("-"+prevToken.getTokenName());
            expression.add("local["+(localVariableCounter-1)+"]");
            return true;
        }

        else if (isNumber())
        {
            makeLocalVariableAndWriteToBuffer(prevToken.getTokenName());
            expression.add("local["+(localVariableCounter-1)+"]");

            return true;
        }

        appendToBuffer = false;      //To print IDs

        //TODO
        if (ID())
        {
            String eitherIDorFunction = getStringFromMap(prevToken.getTokenName());

            System.out.println("Getting for:"+prevToken.getTokenName());

            expression.add(eitherIDorFunction);

            if (factor_prime())
            {
                appendToBuffer = originalState;
                return true;
            }
            appendToBuffer = originalState;
        }

        appendToBuffer = originalState;
        return false;
    }

    private boolean returnStatement_prime()
    {
        boolean originalState = appendToBuffer;

        ArrayList<String> expression = new ArrayList<>();

        //Just a "return ;"
        if (matchToken(";"))
        {
            functionBuffer.append("return;\n");
            appendToBuffer = originalState;

            return true;
        }

        //Evaluating a COMPLEX RETURN => return (2*3); OR return a;
        else if (expression(expression))
        {
            appendToBuffer = false;
            if (matchToken(";"))
            {
                if (expression.size() > 0)
                    functionBuffer.append("return " + evaluate(expression)+";\n");

                appendToBuffer = originalState;
                return true;
            }
        }

        appendToBuffer = originalState;
        return false;
    }

    private boolean returnStatement()
    {
        appendToBuffer = false;

        if (matchToken("return") && returnStatement_prime())
        {
            appendToBuffer = true;
            return true;
        }
        appendToBuffer = true;
        return false;
    }

    private boolean whileStatement()
    {
        appendToBuffer = false;

        if (matchToken("while"))
        {
            //Make the WHILE branch and Stack it
            makeBranchAndStackItUp(false);

            //Put the re-entry label at the top
            String tempCode = labelStack.peek().reEntry+":;\n"+"if ";
            functionBuffer.append(tempCode);

            appendToBuffer = true;

            return (matchToken("(") && conditionExpression() && matchToken(")") && blockStatements());
        }
        appendToBuffer = true;
        return false;
    }

    private boolean ifStatement()
    {
        appendToBuffer = false;

        if (matchToken("if") && matchToken("(") && conditionExpression() && matchToken(")"))
        {
            //Make the Branch and Stack it


            makeBranchAndStackItUp(true);

            return blockStatements();
        }
        return false;
    }

    private boolean conditionExpression_prime()
    {
        if (conditionOperator())
            return condition();

        return true;
    }

    private boolean conditionExpression()
    {
        return (condition() && conditionExpression_prime());
    }

    private boolean condition()
    {
        ArrayList<String> expression = new ArrayList<>();

        return (expression(expression) && comparisonOperator() && expression(expression));
    }

    private boolean functionCall()
    {
        return (matchToken("(") && expression_list() && matchToken(")") && matchToken(";"));
    }

    private boolean assignment(String id)
    {
        //String identifier = getStringFromMap(prevToken.getTokenName());
        System.out.println("ID:"+id);
        String tempString = getStringFromMap(id);

        ArrayList<String> expression = new ArrayList<>();

        if (!(ID_prime() && matchToken("=")))
        {
            return false;
        }

        tempString+=" = ";

        if (!expression(expression))
        {
            return false;
        }

        if (expression.size() > 0)
            tempString+=(evaluate(expression));

        appendToBuffer = false;
        if (matchToken(";"))
        {
            tempString+=";\n";
            functionBuffer.append(tempString);
            appendToBuffer = true;
            return true;
        }
        appendToBuffer = true;
        return false;
    }

    private boolean statement_prime(String id)
    {
        return (assignment(id) || functionCall());
    }

    private boolean statement()
    {
        ArrayList<String> expression = new ArrayList<>();
        appendToBuffer = false;

        if ((ID()))
        {
            if (statement_prime(prevToken.getTokenName()))
                return true;

            return false;
        }

        else
        {
            appendToBuffer = true;

            return (ifStatement() ||
                    whileStatement() ||
                    returnStatement() ||
                    breakStatement() ||
                    continueStatement() ||
                    (matchToken("read") && matchToken("(") && ID() && matchToken(")") && matchToken(";")) ||
                    (matchToken("write") && matchToken("(") && expression(expression) && matchToken(")") && matchToken(";")) ||
                    (matchToken("print") && matchToken("(") && isString() && matchToken(")") && matchToken(";"))
            );
        }
    }

    private boolean statements()
    {
        if (statement())
        {
            statementCounter++;
            return statements();
        }

        return true;
    }

    //Called ONLY from an IF or WHILE statement
    private boolean blockStatements()
    {
        Branch currentBranchOnTop = labelStack.peek();

        //Generating Branching code to be written before statements of the branch start
        String tempCode = "goto "+currentBranchOnTop.entry+";\n"+"goto "+currentBranchOnTop.exit+";\n"+currentBranchOnTop.entry+":;\n";
        functionBuffer.append(tempCode);

        appendToBuffer = false;

        if (matchToken("{"))
        {
            appendToBuffer = true;

            if (!statements())
            {
                return false;
            }
            appendToBuffer = false;

            if (matchToken("}"))
            {
                appendToBuffer = true;

                Branch topBranch = labelStack.pop();
                String exitCode="";

                //for WHILE condition, put the Re-entry label
                if (topBranch.reEntry != null)
                {
                    exitCode+= "goto "+topBranch.reEntry+";\n";

                    //Since this branch is a WHILE loop, pop from whileStack as well
                    whileStack.pop();
                }

                //Exit condition label of the Branch
                exitCode += topBranch.exit+":;\n";

                functionBuffer.append(exitCode);

                return true;
            }
        }
        return false;
    }

    private boolean nonEmptyExpressionList_prime()
    {
        ArrayList<String> expression = new ArrayList<>();

        if (matchToken(","))
            return (expression(expression) && nonEmptyExpressionList_prime());

        return true;
    }

    private boolean nonEmptyExpressionList()
    {
        ArrayList<String> expression = new ArrayList<>();
        return (expression(expression) && nonEmptyExpressionList_prime());
    }

    private boolean expression_list()
    {
        return (nonEmptyExpressionList() || true);
    }

    private boolean expression_prime(ArrayList<String> expression)
    {
        if (addOp(expression))
            return (term(expression) && expression_prime(expression));

        return true;
    }

    private boolean expression(ArrayList<String> expression)
    {
        return (term(expression) && expression_prime(expression));
    }

    private boolean dataDeclaration()
    {
        if (typeName())
            return (ID_list() && matchToken(";") && dataDeclaration());

        return true;
    }

    private boolean ID_list_prime()
    {
        if (matchToken(","))
            return (ID() && ID_list_prime());

        return true;
    }

    private boolean ID_list()
    {
        return (ID() && ID_list_prime());
    }

    private boolean ID_prime()
    {
        ArrayList<String> expression = new ArrayList<>();

        if (matchToken("["))
            return (expression(expression) && matchToken("]"));

        return true;
    }

    private  boolean ID()
    {
        //Get Token before it is updated after isIdentifier()
        String tempID = currentToken.getTokenName();

        if (isIdentifier())
        {
            //1st Global variable
            if (!globalVariablesFinished)
            {
                firstGlobal = tempID;
            }

            //Local Table
            if (isLocal && declareVariables)
            {
                putInLocalMap(tempID);

                //Since it is parameter, write without substituting
                if (inParameterList)
                {
                    //System.out.println("Hello");
                    functionBuffer.append(tempID);
                }
            }
            //Global Table
            else if (!isLocal && declareVariables)
            {
                putInGlobalMap(tempID);
            }

            //Since this is a variable already declared OR this is a function name => Both will be handled by getStringFromMap()
            //EXCEPTION: The 1st Global variable => Shouldn't write to buffer
            else if (appendToBuffer)
            {
                functionBuffer.append(getStringFromMap(tempID));
            }

            variableCounter++;

            return ID_prime();
        }
        return false;
    }

    private boolean non_empty_list_prime()
    {
        if (matchToken(","))
            return (typeName() && ID() && non_empty_list_prime());

        return true;
    }

    private boolean non_empty_list()
    {
        return (typeName() && ID() && non_empty_list_prime());
    }

    private boolean parameter_list()
    {
        return (matchToken("void")||
                non_empty_list() ||
                true);
    }

    private boolean functionDeclaration()
    {
        if (typeName() && ID() && matchToken("("))
        {
            declareVariables = true;
            isLocal = true;
            inParameterList = true;

            //Handle Parameter processing JUST after parameter_list()
            return (parameter_list() && matchToken(")"));
        }
        return false;
    }

    //<func_prime> —> semicolon | left_brace <data_decl> <statements> right_brace
    private boolean function_prime()
    {
        //Parameters are Visited
        inParameterList = false;

        if (matchToken(";"))
            return true;

        else  if ((matchToken("{")))
        {
            //Append All arguments, and local[$]
            appendToBuffer = false;

            //Write the Generated Code from Parameter list
            makeCodeFromParameters();

            if (dataDeclaration())
            {
                //All IDs used after this would not be Declaration
                declareVariables = false;

                //Since Data Declarations for the function is over, START APPENDING again!
                appendToBuffer = true;

                if (statements() && matchToken("}"))
                {
                    //Clear local count, Local Symbol tables and Process it
                    clearLocalData();

                    //isLocal = false;
                    functionCounter++;

                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    private boolean function()
    {
        return (functionDeclaration() && function_prime());
    }

    private boolean functionList()
    {
        if (function())
            return functionList();

        return true;
    }

    private boolean program_prime_2()
    {
        //This means we have our 1st function of the program!
        if (matchToken("("))
        {
            //Make the "int global" code from Global Map here
            makeCodeFromGlobalMap();

            declareVariables = true;
            isLocal = true;
            inParameterList = true;

            return (parameter_list() && matchToken(")") && function_prime() && functionList());
        }

        //This can be list of global variables, followed by functions
        else
        {
            //Calling Global Map explicitly for 1st variables (Eg: int x,y; => x)
            putInGlobalMap(firstGlobal);

            //Clear Buffer since we don't want the "int"
            functionBuffer = new StringBuilder();

            declareVariables = true;
            appendToBuffer = false;

            //Handle global Arrays later using ID_prime()
            if (!(ID_prime() && ID_list_prime() && matchToken(";")))
            {
                return false;
            }

            appendToBuffer = true;
            declareVariables = false;

            variableCounter++;

            return program_prime();
            //return true;
        }
    }


    private boolean program_prime()
    {
        if (typeName())
            return (ID() && program_prime_2());

        return true;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Parsing Starts here~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public boolean start()
    {
        if (currentToken.getTokenType() == TokenType.EOF)       //Empty program is correct
            return true;

        if (program_prime() && (currentToken.getTokenType() == TokenType.EOF))
        {
            try {
                outputWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error in Closing output file");
            }
            return true;
        }
        return false;
    }
}
