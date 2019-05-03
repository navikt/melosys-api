package no.nav.melosys.service.dokument.brev.mapper.felles;

import java.time.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public final class BrevMapperUtils {

    private BrevMapperUtils() {
    }

    public static XMLGregorianCalendar lagXmlDato(LocalDate dato) {
        try {
            return convertToXMLGregorianCalendarRemoveTimezone(dato);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Feil ved konvertering av Instant til XmlGregorianCalendar", e);
        }
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendarRemoveTimezone(LocalDate localDate) throws DatatypeConfigurationException {
        if (localDate == null) {
            return null;
        }
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(
            localDate.getYear(),
            localDate.getMonthValue(),
            localDate.getDayOfMonth(),
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED
        );
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendarRemoveTimezone(Instant instant) {
        if (instant == null) {
            return null;
        }
        try {
            return convertToXMLGregorianCalendarRemoveTimezone(LocalDate.from(LocalDateTime.ofInstant(instant, ZoneId.systemDefault())));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Feil ved konvertering av Instant til XmlGregorianCalendar", e);
        }
    }

}
