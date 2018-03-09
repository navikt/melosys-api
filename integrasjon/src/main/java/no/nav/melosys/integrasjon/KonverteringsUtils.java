package no.nav.melosys.integrasjon;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
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

    public static LocalDateTime xmlGregorianCalendarToLocalDateTime(XMLGregorianCalendar xmlGregorianCalendar) {
        if (xmlGregorianCalendar == null) {
            return null;
        }
        return LocalDateTime.ofInstant(xmlGregorianCalendar.toGregorianCalendar().getTime().toInstant(), ZoneId.systemDefault());
    }

    public static LocalDate xmlGregorianCalendarToLocalDate(XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }
        return calendar.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }
}
