package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.xml.datatype.XMLGregorianCalendar;

public class DtoUtils {

    public static LocalDate tilLocalDate(XMLGregorianCalendar xmlCal) {
        if (xmlCal == null) {
            return null;
        }
        return xmlCal.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }

    public static LocalDateTime tilLocalDateTime(XMLGregorianCalendar tidspunkt) {
        if (tidspunkt == null) {
            return null;
        }
        return tidspunkt.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
    }

}
