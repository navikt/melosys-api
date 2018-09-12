package no.nav.melosys.integrasjon;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class KonverteringsUtils {

    public static XMLGregorianCalendar localDateTimeToXMLGregorianCalendar(LocalDateTime localDateTime) throws DatatypeConfigurationException {
        if (localDateTime == null) {
            return null;
        }
        final DatatypeFactory factory = DatatypeFactory.newInstance();
        return factory.newXMLGregorianCalendar(localDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
    }

    public static XMLGregorianCalendar localDateToXMLGregorianCalendar(LocalDate dateTime) throws DatatypeConfigurationException {
        if (dateTime == null) {
            return null;
        }
        final DatatypeFactory factory = DatatypeFactory.newInstance();
        return factory.newXMLGregorianCalendar(dateTime.format(DateTimeFormatter.ISO_DATE));
    }

    public static Instant xmlGregorianCalendarToInstant(XMLGregorianCalendar xmlGregorianCalendar) {
        if (xmlGregorianCalendar == null) {
            return null;
        }
        return xmlGregorianCalendar.toGregorianCalendar().toZonedDateTime().withZoneSameLocal(ZoneId.systemDefault()).toInstant();
    }

    /*
        Default datoformat er definert som:
        - "uuuu-MM-dd" for java.util.LocalDate.toString()/.parse()
        - "yyyy-MM-dd" for org.joda.time.LocalDate.toString()/.parse()
        Håndteres likt fordi:
        - Java definerer "u" som "year" og "y" som "year of era" (https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html),
        - Joda-Time definerer "y" som "year" og "Y" som "year of era" (http://www.joda.org/joda-time/key_format.html),
    */
    public static org.joda.time.LocalDate javaLocalDateToJodaLocalDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return org.joda.time.LocalDate.parse(date.toString());
    }

    public static LocalDate jodaLocalDateToJavaLocalDate(org.joda.time.LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDate.parse(date.toString());
    }
}
