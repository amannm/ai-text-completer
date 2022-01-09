package systems.cauldron.completion.tokenizer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TokenizerTest {

    private static final String OPENAPI_EXAMPLE =
            """
                    Many words map to one token, but some don't: indivisible.
                                
                    Unicode characters like emojis may be split into many tokens containing the underlying bytes: 🤚🏾
                                
                    Sequences of characters commonly found next to each other may be grouped together: 1234567890
                    """;
    public static final int OPENAPI_EXAMPLE_TOKEN_COUNT = 64;

    @Test
    public void basicTest() {
        Gpt3Tokenizer tokenizer = new Gpt3Tokenizer();
        List<String> tokens = tokenizer.tokenize(OPENAPI_EXAMPLE);
        assertEquals(OPENAPI_EXAMPLE_TOKEN_COUNT, tokens.size());
    }
}

