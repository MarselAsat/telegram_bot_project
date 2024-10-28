package ru.telproject.utils;

import lombok.experimental.UtilityClass;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;

@UtilityClass
public class ModelTrainer {
    public void trainAndSaveModel(String trainingDataModelPath, String modelPath) throws IOException {
        MarkableFileInputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(
                ResourceUtils.getFile(trainingDataModelPath));
        PlainTextByLineStream lineStream = new PlainTextByLineStream(inputStreamFactory, "UTF-8");
        DocumentSampleStream sampleStream = new DocumentSampleStream(lineStream);

        TrainingParameters parameters = new TrainingParameters();
        parameters.put(TrainingParameters.ITERATIONS_PARAM, 300);
        parameters.put(TrainingParameters.CUTOFF_PARAM, 5);

        DoccatModel model = DocumentCategorizerME.train("ru", sampleStream, parameters, new DoccatFactory());
        model.serialize(new File(modelPath));
    }
}
