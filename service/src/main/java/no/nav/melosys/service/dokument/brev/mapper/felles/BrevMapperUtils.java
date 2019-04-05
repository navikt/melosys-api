package no.nav.melosys.service.dokument.brev.mapper.felles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melosys.domain.Lovvalgsperiode;

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

    public static void validerLovvalgsperioder(Set<Lovvalgsperiode> perioder) {
        if (perioder.size() != 1) {
            throw new UnsupportedOperationException(String.format("Antall lovvalgsperioder (%s) ulik 1 støttes ikke i første versjon av Melosys.",
                perioder.size()));
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
            return convertToXMLGregorianCalendarRemoveTimezone(LocalDate.from(LocalDateTime.ofInstant(instant, ZoneOffset.UTC)));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Feil ved konvertering av Instant til XmlGregorianCalendar", e);
        }
    }

}
