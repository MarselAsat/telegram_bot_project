package ru.telproject.utils;

import lombok.experimental.UtilityClass;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TextFinderUtils {
    private static final Locale RU_LOCALE = new Locale("ru");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy d MMMM", RU_LOCALE);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final String REGEX_DATE = "(\\d{1,2})\\s+(января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря)";
    private static final String REGEX_WORD_DATE = "(сегодня|завтра)";
    private static final String REGEX_TIME = "(\\d{1,2}):(\\d{2})";

    private static final Pattern DATE_PATTERN = Pattern.compile(REGEX_DATE, Pattern.CASE_INSENSITIVE);
    private static final Pattern WORD_DATE_PATTERN = Pattern.compile(REGEX_WORD_DATE, Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATTERN = Pattern.compile(REGEX_TIME);

    public LocalDate getDateFromString(@NonNull String messageText) {
        try {
            if (messageText.contains("сегодня")) {
                return parseWordDate(0);
            } else if (messageText.contains("завтра")) {
                return parseWordDate(1);
            }
            return parseRelativeDate(messageText);
        }catch (Exception e){
            throw new DateTimeParseException("Неправильный формат даты", messageText, 0);
        }
    }
    public List<LocalDate> extractAllDates(@NonNull String messageText) {
        List<LocalDate> allDate = new ArrayList<>();
        Matcher dateMatcher = DATE_PATTERN.matcher(messageText);
        while (dateMatcher.find()){
            allDate.add(normalFormDate(dateMatcher.group()));
        }
        Matcher wordMatcher = WORD_DATE_PATTERN.matcher(messageText);
        while (wordMatcher.find()) {
            String word = wordMatcher.group().toLowerCase();
            allDate.add(word.equals("сегодня") ? LocalDate.now() : LocalDate.now().plusDays(1));
        }
        return allDate;
    }
    public LocalTime getLocalTimeFromString(String text) {
        Matcher timeMatcher = TIME_PATTERN.matcher(text);
        if (!timeMatcher.find()){
            throw new DateTimeParseException("Время не найдено", text, 0);
        }
        return LocalTime.parse(timeMatcher.group(), TIME_FORMATTER);
    }
    public List<LocalTime> getAllLocalTimeFromString(String text) {
        List<LocalTime> allTime = new ArrayList<>();
        Matcher timeMatcher = TIME_PATTERN.matcher(text);
        while (timeMatcher.find()){
            try {
                allTime.add(LocalTime.parse(timeMatcher.group(), TIME_FORMATTER));
            }catch (DateTimeParseException ignored){}
        }
        return allTime;
    }

    public Matcher getMatcherAnExpression(String regex, String text) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(text);
    }

    public Matcher findRecordOnText(String regex, String text){
        Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(text);
        return matcher;
    }

    private LocalDate parseWordDate(int daysToAdd){
        return LocalDate.now().plusDays(daysToAdd);
    }

    private LocalDate parseRelativeDate(String text) {
        Matcher dateMatcher = getMatcherAnExpression(REGEX_DATE, text);
        dateMatcher.find();
        return normalFormDate(dateMatcher.group());
    }

    private LocalDate normalFormDate(String text){
        String yearString = String.valueOf(LocalDate.now().getYear());
        return LocalDate.parse(yearString + " " + text, DATE_FORMATTER);
    }


}
