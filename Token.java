public class Token {

    private TokenType tokenType;
    private String tokenName;

    Token(TokenType type, String name)
    {
        tokenType = type;
        tokenName = name;
    }

    public TokenType getTokenType()
    {
        return tokenType;
    }

    public String getTokenName()
    {
        return tokenName;
    }
}
