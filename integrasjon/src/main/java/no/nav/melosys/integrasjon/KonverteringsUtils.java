package no.nav.melosys.integrasjon;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class KonverteringsUtils {

    public static XMLGregorianCalendar localDateToXMLGregorianCalendar(LocalDate date) throws DatatypeConfigurationException {
        final DatatypeFactory factory = DatatypeFactory.newInstance();
        return factory.newXMLGregorianCalendar(date.format(DateTimeFormatter.ISO_DATE));
    }
}
