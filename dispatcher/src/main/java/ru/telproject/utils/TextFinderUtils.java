package ru.telproject.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TextFinderUtils {
    private final String regexDate = "(\\d{1,2})\\s+(января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря)";
    private final String regexWordDate = "(сегодня|завтра)";
    public LocalDate getDateFromString(String messageText) {
        LocalDate date = null;
        Matcher dateMatcher = getMatcherAnExpression(regexDate, messageText);
        Matcher wordMatcher = getMatcherAnExpression(regexWordDate, messageText);
        String yearString = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy d MMMM", new Locale("ru"));
        if (dateMatcher.find()){
            date = LocalDate.parse(yearString + " " + dateMatcher.group(), dateFormatter);
        }else if (wordMatcher.find()) {
            date = parseWordDate(wordMatcher.group());
        }
        return date;
    }
    public List<LocalDate> getAllDateFromString(String messageText) {
        List<LocalDate> allDate = new ArrayList<>();
        Matcher dateMatcher = getMatcherAnExpression(regexDate, messageText);
        Matcher wordMatcher = getMatcherAnExpression(regexWordDate, messageText);
        String yearString = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy d MMMM", new Locale("ru"));
        while (dateMatcher.find()){
             allDate.add(LocalDate.parse(yearString + " " + dateMatcher.group(), dateFormatter));
        }
        while (wordMatcher.find()) {
            allDate.add(parseWordDate(wordMatcher.group()));
        }
        return allDate;
    }
    public LocalTime getLocalTimeFromString(String text) {
        Pattern patternTime = Pattern.compile("(\\d{1,2}):(\\d{2})");
        Matcher timeMatcher = patternTime.matcher(text);
        String time = "";
        if (timeMatcher.find()){
            time = timeMatcher.group();
        }
        LocalTime parseTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        return parseTime;
    }
    public List<LocalTime> getAllLocalTimeFromString(String text) {
        Pattern patternTime = Pattern.compile("(\\d{1,2}):(\\d{2})");
        Matcher timeMatcher = patternTime.matcher(text);
        String time = "";
        List<LocalTime> allTime = new ArrayList<>();
        while (timeMatcher.find()){
            time = timeMatcher.group();
            allTime.add(LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm")));
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

    private LocalDate parseWordDate(String text){
        LocalDate date = null;
        switch (text.toLowerCase()){
            case "сегодня":
                date = LocalDate.now();
                break;
            case "завтра":
                date = LocalDate.now().plusDays(1);
        }
        return date;
    }

}
