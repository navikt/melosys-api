package no.nav.melosys.integrasjon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;


public final class KonverteringsUtils {

    private KonverteringsUtils() {
        throw new IllegalArgumentException("Utility");
    }

    public static XMLGregorianCalendar localDateTimeToXMLGregorianCalendar(LocalDateTime localDateTime) throws DatatypeConfigurationException {
        if (localDateTime == null) {
            return null;
        }
        final DatatypeFactory factory = DatatypeFactory.newInstance();
        return factory.newXMLGregorianCalendar(localDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
    }

    public static LocalDate localDateTimeToLocalDate(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return date.toLocalDate();
    }
}
