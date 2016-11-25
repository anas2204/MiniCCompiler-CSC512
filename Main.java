

import java.io.*;

public class Main {

    public static void main(String[] args) throws IOException
    {

        if (args.length != 1)
        {
            System.out.println("INCORRECT FORMAT: Try -> java Parser <fileName>");
            System.exit(0);
        }

        ParserLatest parser = new ParserLatest(args[0]);
        boolean result = parser.start();

        if (result)
            System.out.println(" Pass variable "+parser.getVariableCounter()+" function "+parser.getFunctionCounter()+
                    " statement "+parser.getStatementCounter());

        else
            System.out.println(" Error in source file");
    }
}
