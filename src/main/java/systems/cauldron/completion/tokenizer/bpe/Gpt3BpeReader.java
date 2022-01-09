package systems.cauldron.completion.tokenizer.bpe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Gpt3BpeReader {

    private final Map<SymbolPair, Integer> pairLookup;

    public Gpt3BpeReader() {
        Map<Integer, Integer> codepointMapping = computeCodepointMap();
        List<SymbolPair> allPairs;
        InputStream is = getClass().getClassLoader().getResourceAsStream("gpt3-vocab.bpe");
        if (is == null) {
            throw new RuntimeException("failed to load required GPT3 BPE file");
        }
        try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            allPairs = br.lines()
                    .skip(1)
                    .map(line -> {
                        String[] pair = line.split("\\s");
                        if (pair.length != 2) {
                            throw new RuntimeException("malformed line in BPE file: " + line);
                        }
                        String first = rectifyString(pair[0], codepointMapping);
                        String second = rectifyString(pair[1], codepointMapping);
                        return new SymbolPair(first, second);
                    })
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.pairLookup = IntStream.range(0, allPairs.size()).boxed()
                .collect(Collectors.toUnmodifiableMap(allPairs::get, Function.identity()));
    }

    public SymbolPair selectMinRankPair(Set<SymbolPair> pairs) {
        int minRank = Integer.MAX_VALUE;
        SymbolPair minPair = null;
        for (SymbolPair pair : pairs) {
            Integer rank = pairLookup.get(pair);
            if (rank != null) {
                if (rank < minRank) {
                    minRank = rank;
                    minPair = pair;
                }
            }
        }
        return minPair;
    }

    private static Map<Integer, Integer> computeCodepointMap() {
        HashMap<Integer, Integer> codepointMapping = new HashMap<>();
        int extraOffset = 256;
        for (int i = 0; i < 256; i++) {
            if (i < 33 || (i > 126 && i < 161) || i == 173) {
                codepointMapping.put(extraOffset, i);
                extraOffset++;
            } else {
                codepointMapping.put(i, i);
            }
        }
        return codepointMapping;
    }

    private static String rectifyString(String value, Map<Integer, Integer> codepointMapping) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            int codePoint = value.codePointAt(i);
            int remappedCodePoint = codepointMapping.get(codePoint);
            sb.appendCodePoint(remappedCodePoint);
        }
        return sb.toString();
    }
}