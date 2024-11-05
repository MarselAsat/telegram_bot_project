package ru.telproject.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
@DisplayName("Tests for DateTimeExtractor")
class TextFinderUtilsTest {

    @Test
    void getDateFromStringToday() {
        String textToday = "запись сегодня в 15:00";
        LocalDate today = TextFinderUtils.getDateFromString(textToday);
        assertEquals(LocalDate.now(), today);
    }
    @Test
    void getDateFromStringTomorrow() {
        String textTomorrow = "запись завтра в 15:00";
        LocalDate tomorrow = TextFinderUtils.getDateFromString(textTomorrow);
        assertEquals(LocalDate.now().plusDays(1),tomorrow);
    }
    @Test
    void getDateFromStringSpecificDate() {
        String text = "запись 25 ноября в 15:00";
        LocalDate date = TextFinderUtils.getDateFromString(text);
        assertEquals(LocalDate.of(LocalDate.now().getYear(), 11, 25),date);
    }

    @Test
    void extractAllDates() {
        String text = "перенеси запись с завтра на 25 октября ";
        List<LocalDate> localDates = TextFinderUtils.extractAllDates(text);
        assertEquals(LocalDate.now().plusDays(1), localDates.get(0));
        assertEquals(LocalDate.now().withMonth(10).withDayOfMonth(25), localDates.get(1));
    }

    @Test
    void getLocalTimeFromString() {
        String text = "запись завтра в 15:00";
        LocalTime time = TextFinderUtils.getLocalTimeFromString(text);
        assertEquals(LocalTime.of(15,0), time);
    }    @Test
    void getLocalTimeFromStringException() {
        String text = "запись завтра в произвольное время";
        assertThrows(DateTimeParseException.class, () -> TextFinderUtils.getLocalTimeFromString(text));
    }

    @Test
    void getAllLocalTimeFromString() {
        String text = "перенеси запись с завтра с 15:00 на 25 октября 16:00";
        List<LocalTime> allLocalTimeFromString = TextFinderUtils.getAllLocalTimeFromString(text);
        assertEquals(LocalDate.now().plusDays(1).atTime(15, 0), allLocalTimeFromString.get(0));
        assertEquals(LocalDate.of(LocalDate.now().getYear(), 10, 25), allLocalTimeFromString.get(1));
    }

    @Test
    void findRecordOnText() {
        String text = "запись на макияж в 12:00";
        Matcher matcher = TextFinderUtils.findRecordOnText("(макияж)", text);
        assertEquals("макияж", matcher.find() ? matcher.group(1) : null);
    }

}