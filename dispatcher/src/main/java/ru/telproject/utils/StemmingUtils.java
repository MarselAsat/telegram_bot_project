package ru.telproject.utils;

import lombok.experimental.UtilityClass;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class StemmingUtils {
    public List<String> stemWords(List<String> words)throws IOException {
        List<String> stemmedWords = new ArrayList<>();
        RussianAnalyzer russianAnalyzer = new RussianAnalyzer();
        for (String word : words){
            TokenStream tokenStream = russianAnalyzer.tokenStream("content", word);
            CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            while (tokenStream.incrementToken()){
                stemmedWords.add(attr.toString());
            }
            tokenStream.end();
            tokenStream.close();
        }
        russianAnalyzer.close();
        return stemmedWords;
    }
}
