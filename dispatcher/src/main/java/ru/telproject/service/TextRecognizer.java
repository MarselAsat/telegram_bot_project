package ru.telproject.service;

import lombok.RequiredArgsConstructor;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.telproject.utils.ModelTrainer;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class TextRecognizer {
    @Value("${intent.model.path}")
    private String intentModelPath;

    @Value("${intent.training.data.path}")
    private String intentTrainingDataResource;


    private DocumentCategorizerME intentCategorizer;
    private final MetricsService metricsService;

    @PostConstruct
    public void init() throws IOException {
        ModelTrainer.trainAndSaveModel(intentTrainingDataResource, intentModelPath);
        FileInputStream intentStream = new FileInputStream(intentModelPath);
        DoccatModel intentModel = new DoccatModel(intentStream);
        intentCategorizer = new DocumentCategorizerME(intentModel);
    }

    public String recognizeIntent(String text){
        return metricsService.recordingTime("recognize_text_time", () -> {
            String[] tokens = SimpleTokenizer.INSTANCE.tokenize(text.toLowerCase());
            double[] outcomes = intentCategorizer.categorize(tokens);
            String bestCategory = intentCategorizer.getBestCategory(outcomes);
            double maxResult = Arrays.stream(outcomes).max().getAsDouble();
            if (maxResult < 0.5) {
                bestCategory = "null";
            }
            metricsService.incrementCounter("recognize_text_successful_counter", "intent_user",
                    bestCategory);
            return bestCategory;
        });
    }
}
