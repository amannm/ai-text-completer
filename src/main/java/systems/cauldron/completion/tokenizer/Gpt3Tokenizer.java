package systems.cauldron.completion.tokenizer;

import systems.cauldron.completion.tokenizer.bpe.Gpt3BpeReader;
import systems.cauldron.completion.tokenizer.bpe.SymbolPair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Gpt3Tokenizer implements Tokenizer {

    private static final Pattern GPT3_PRETOKEN_PATTERN = Pattern.compile("'s|'t|'re|'ve|'m|'ll|'d| ?\\p{L}+| ?\\p{N}+| ?[^\\s\\p{L}\\p{N}]+|\\s+(?!\\S)|\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private final Gpt3BpeReader pairLookup;
    private final Map<String, List<String>> tokenCache;

    public Gpt3Tokenizer() {
        this.pairLookup = new Gpt3BpeReader();
        this.tokenCache = new ConcurrentHashMap<>();
    }

    public List<String> tokenize(String text) {
        List<String> result = new ArrayList<>();
        Matcher preTokenMatcher = GPT3_PRETOKEN_PATTERN.matcher(text);
        while (preTokenMatcher.find()) {
            String preToken = preTokenMatcher.group();
            List<String> cachedTokens = tokenCache.computeIfAbsent(preToken, x -> {
                List<String> tokens = initializeTokens(preToken);
                while (true) {
                    Set<SymbolPair> pairs = generatePairs(tokens);
                    SymbolPair minRankPair = pairLookup.selectMinRankPair(pairs);
                    if (minRankPair == null) {
                        break;
                    }
                    tokens = mergeTokensByPair(minRankPair, tokens);
                }
                return tokens;
            });
            result.addAll(cachedTokens);
        }
        return result;
    }

    private static List<String> initializeTokens(String preToken) {
        List<String> result = new ArrayList<>(preToken.length());
        for (int i = 0; i < preToken.length(); i++) {
            result.add(preToken.substring(i, i + 1));
        }
        return result;
    }

    private static Set<SymbolPair> generatePairs(List<String> tokens) {
        Set<SymbolPair> pairs = new HashSet<>();
        for (int i = 1; i < tokens.size(); i++) {
            String previous = tokens.get(i - 1);
            String current = tokens.get(i);
            SymbolPair pair = new SymbolPair(previous, current);
            pairs.add(pair);
        }
        return pairs;
    }

    private static List<String> mergeTokensByPair(SymbolPair pair, List<String> tokens) {
        List<String> newTokens = new ArrayList<>();
        int sourceLength = tokens.size();
        for (int i = 0; i < sourceLength; i++) {
            String current = tokens.get(i);
            if (i < sourceLength - 1 && current.equals(pair.first())) {
                String next = tokens.get(i + 1);
                if (next.equals(pair.second())) {
                    newTokens.add(pair.merged());
                    i++;
                    continue;
                }
            }
            newTokens.add(current);
        }
        return newTokens;
    }
}
