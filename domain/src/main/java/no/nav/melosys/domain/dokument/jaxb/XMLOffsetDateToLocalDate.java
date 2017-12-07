package no.nav.melosys.domain.dokument.jaxb;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class XMLOffsetDateToLocalDate extends AbstractDateXmlAdapter<LocalDate> {

    public XMLOffsetDateToLocalDate() {
        super(DateTimeFormatter.ISO_OFFSET_DATE, LocalDate::from);
    }
}
