package no.nav.melosys.tjenester.gui.dto.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.datatype.XMLGregorianCalendar;

public final class DtoUtils {

    private static final String ISO_DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE_PATTERN);

    private DtoUtils() {
    }

    public static LocalDate tilLocalDate(XMLGregorianCalendar xmlCal) {
        if (xmlCal == null) {
            return null;
        }
        return xmlCal.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }

    public static LocalDate tilLocalDate(org.joda.time.LocalDate jodaLocalDate) {
        String dateStr = jodaLocalDate.toString(ISO_DATE_PATTERN);
        return LocalDate.parse(dateStr, ISO_DATE_FORMATTER);
    }

    public static LocalDateTime tilLocalDateTime(XMLGregorianCalendar tidspunkt) {
        if (tidspunkt == null) {
            return null;
        }
        return tidspunkt.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
    }

}
