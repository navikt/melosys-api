package no.nav.melosys.integrasjon;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class KonverteringsUtils {

    public static XMLGregorianCalendar localDateTimeToXMLGregorianCalendar(LocalDateTime dateTime) throws DatatypeConfigurationException {
        final DatatypeFactory factory = DatatypeFactory.newInstance();
        return factory.newXMLGregorianCalendar(dateTime.format(DateTimeFormatter.ISO_DATE_TIME));
    }
}
