import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public class Parser {

    private Scanner scanner;
    Token currentToken = null;

    //File Containing Meta statements
    BufferedWriter outputWriter;

    //Program Counters
    private int variableCounter;
    private int functionCounter;
    private int statementCounter;

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

        //Initializing Counters to 0
        variableCounter = functionCounter = statementCounter = 0;

        //Get the 1st Token
        nextToken();
    }

    //Gets the Next token from the Scanner
    private void nextToken()
    {
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
                outputWriter.close();
            }
        }
        catch (Exception e)
        {
            System.out.println("Exception in nextToken():"+e.getMessage());
        }
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

    //Matches current Token with "str" and gets next Token if match was successful
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
        if (currentToken.getTokenType() == TokenType.NUMBER)
        {
            nextToken();
            return true;
        }
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

    //GRAMMAR STARTS
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
        return (matchToken("break") && matchToken(";"));
    }

    private boolean continueStatement()
    {
        return (matchToken("continue") && matchToken(";"));
    }

    private boolean addOp()
    {
        return (matchToken("+") || matchToken("-"));
    }

    private boolean mulOp()
    {
        return (matchToken("*") || matchToken("/"));
    }

    private boolean term_prime()
    {
        if (mulOp())
            return (factor() && term_prime());

        return true;
    }

    private boolean term()
    {
        return (factor() && term_prime());
    }

    private boolean factor_prime()
    {
        if (matchToken("["))
            return (expression() && matchToken("]"));

        else  if (matchToken("("))
            return (expression_list() && matchToken(")"));

        else
            return true;
    }

    private boolean factor()
    {
        if (isIdentifier())
            return (factor_prime());

        else if (matchToken("-"))
            return isNumber();

        else if (isNumber())
            return true;

        else if (matchToken("("))
            return (expression() && matchToken(")"));

        return false;

    }

    private boolean returnStatement_prime()
    {
        if (matchToken(";"))
            return true;

        else if (expression())
            return matchToken(";");

        return false;
    }

    private boolean returnStatement()
    {
        return (matchToken("return") && returnStatement_prime());
    }

    private boolean whileStatement()
    {
        return (matchToken("while") && matchToken("(") && conditionExpression() && matchToken(")") && blockStatements());
    }

    private boolean ifStatement()
    {
        return (matchToken("if") && matchToken("(") && conditionExpression() && matchToken(")") && blockStatements());
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
        return (expression() && comparisonOperator() && expression());
    }

    private boolean functionCall()
    {
        return (matchToken("(") && expression_list() && matchToken(")") && matchToken(";"));
    }

    private boolean assignment()
    {
        return (ID_prime() && matchToken("=") && expression() && matchToken(";"));
    }

    private boolean statement_prime()
    {
        return (assignment() || functionCall());
    }

    private boolean statement()
    {
        return ((isIdentifier() && statement_prime()) ||
                ifStatement() ||
                whileStatement() ||
                returnStatement() ||
                breakStatement() ||
                continueStatement() ||
                (matchToken("read") && matchToken("(") && isIdentifier() && matchToken(")") && matchToken(";")) ||
                (matchToken("write") && matchToken("(") && expression() && matchToken(")") && matchToken(";")) ||
                (matchToken("print") && matchToken("(") && isString() && matchToken(")") && matchToken(";"))
        );
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

    private boolean blockStatements()
    {
        return (matchToken("{") && statements() && matchToken("}"));
    }

    private boolean nonEmptyExpressionList_prime()
    {
        if (matchToken(","))
            return (expression() && nonEmptyExpressionList_prime());

        return true;
    }

    private boolean nonEmptyExpressionList()
    {
        return (expression() && nonEmptyExpressionList_prime());
    }

    private boolean expression_list()
    {
        return (nonEmptyExpressionList() || true);
    }

    private boolean expression_prime()
    {
        if (addOp())
            return (term() && expression_prime());

        return true;
    }

    private boolean expression()
    {
        return (term() && expression_prime());
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
        if (matchToken("["))
            return (expression() && matchToken("]"));

        return true;
    }

    private  boolean ID()
    {
        if (isIdentifier())
        {            
            variableCounter++;

            return ID_prime();
        }
        return false;
    }

    private boolean non_empty_list_prime()
    {
        if (matchToken(","))
            return (typeName() && isIdentifier() && non_empty_list_prime());

        return true;
    }

    private boolean non_empty_list()
    {
        return (typeName() && isIdentifier() && non_empty_list_prime());
    }

    private boolean parameter_list()
    {
        return (matchToken("void")||
                non_empty_list() ||
                true);
    }

    private boolean functionDeclaration()
    {
        return (typeName() && isIdentifier() && matchToken("(") && parameter_list() && matchToken(")"));
    }

    private boolean function_prime()
    {
        if (matchToken(";"))
            return true;

        else  if ((matchToken("{") && dataDeclaration() && statements() && matchToken("}")))
        {
            functionCounter++;
            return true;
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
        if (matchToken("("))
            return (parameter_list() && matchToken(")") && function_prime() && functionList());

        else if (ID_prime() && ID_list_prime() && matchToken(";") && program_prime())
        {
            variableCounter++;
            return true;
        }

        return false;
    }

    private boolean program_prime()
    {
        if (typeName())
            return (isIdentifier() && program_prime_2());

        return true;
    }

    //Parsing Starts here
    public boolean start()
    {
        if (currentToken.getTokenType() == TokenType.EOF)       //Empty program is correct
            return true;

        return (program_prime() && (currentToken.getTokenType() == TokenType.EOF));
    }
}
