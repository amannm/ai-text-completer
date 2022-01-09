package systems.cauldron.completion.tokenizer;

import java.util.List;

public interface Tokenizer {
    List<String> tokenize(String text);
}
