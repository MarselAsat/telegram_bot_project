package ru.telproject.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@Import(TextRecognizer.class)
@TestPropertySource(properties = {
        "intent.training.data.path=src/test/resources/intent-training-data.txt",
        "intent.model.path=src/test/resources/intent-model.bin"
})
class TextRecognizerTest {

    @Autowired
    private TextRecognizer textRecognizer;

    @Nested
    @DisplayName("Тесты распознавания намерений")
    class CommandRecognitionTests {
        @Test
        @DisplayName("Распознавание намерения создания записи")
        void recognizeCreateRecordCommand() {
            List<String> userMessage = List.of("Cоздай запись на маникюр на 14:00 завтра",
                            "Запиши на педикюр на 15:30",
                            "Создай запись на макияж на 11:00",
                            "Запиши на наращивание ресниц на 16:00.",
                            "Создай запись на коррекцию бровей на 13:00");

            List<String> intents = new ArrayList<>();
            for (String message : userMessage) {
                intents.add(textRecognizer.recognizeIntent(message));
            }
            for (String intent : intents) {
                assertTrue(intent.contains("create_record"));
            }
        }

        @Test
        @DisplayName("Распознавание намерения удаления записи")
        void recognizeDeleteRecordCommand() {
            List<String> userMessage = List.of("Отмени запись на маникюр на 14:00 сегодня",
                    "Удали запись на маникюр на 14:00 сегодня",
                    "Вычеркни запись на маникюр");
            List<String> intents = new ArrayList<>();
            for (String message : userMessage) {
                intents.add(textRecognizer.recognizeIntent(message));
            }
            for (String intent : intents) {
                assertTrue(intent.contains("delete_record"));
            }
        }

        @Test
        @DisplayName("Распознавание намерения просмотра записей")
        void recognizeViewRecordCommand() {
            List<String> userMessage = List.of("Покажи все записи на сегодня",
                    "Покажи все записи на завтра",
                    "Покажи все записи на 15 октября");
            List<String> intents = new ArrayList<>();
            for (String message : userMessage) {
                intents.add(textRecognizer.recognizeIntent(message));
            }
            for (String intent : intents) {
                assertTrue(intent.contains("view_records"));
            }
        }

        @Test
        @DisplayName("Распознавание намерения создания типа записи")
        void recognizeCreateTypeRecordCommand() {
            List<String> userMessage = List.of("Создай тип услуги маникюр",
                            "Создай новую услугу педикюр");
            List<String> intents = new ArrayList<>();
            for (String message : userMessage) {
                intents.add(textRecognizer.recognizeIntent(message));
            }
            for (String intent : intents) {
                assertTrue(intent.contains("create_type_record"));
            }
        }
        @Test
        @DisplayName("Распознавание намерения удаления типа записи")
        void recognizeDeleteTypeRecordCommand() {
            List<String> userMessage = List.of("Удали тип услуги стрижка",
                            "Убери услугу педикюр");

            List<String> intents = new ArrayList<>();
            for (String message : userMessage) {
                intents.add(textRecognizer.recognizeIntent(message));
            }
            for (String intent : intents) {
                assertTrue(intent.contains("delete_type_record"));
            }
        }

        @Test
        @DisplayName("Распознавание намерения просмотра типов записей")
        void recognizeViewRecordTypeCommand() {
            List<String> userMessage = List.of("Покажи все услуги",
                            "Какие услуги у меня есть",
                            "Выведи все услуги",
                            "Хочу увидеть все услуги",
                            "Покажи мне услуги");

            List<String> intents = new ArrayList<>();
            for (String message : userMessage) {
                intents.add(textRecognizer.recognizeIntent(message));
            }
            for (String intent : intents) {
                assertTrue(intent.contains("view_type_records"));
            }
        }
    }
    @Test
    @DisplayName("Обработка пустого текста")
    void handleEmptyText() {
        String text = "";

        String intent = textRecognizer.recognizeIntent(text);

        assertEquals("null", intent);
    }
}