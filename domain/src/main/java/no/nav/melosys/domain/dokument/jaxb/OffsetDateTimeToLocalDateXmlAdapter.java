package no.nav.melosys.domain.dokument.jaxb;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeToLocalDateXmlAdapter extends AbstractDateXmlAdapter<LocalDate> {

    public OffsetDateTimeToLocalDateXmlAdapter() {
        super(DateTimeFormatter.ISO_OFFSET_DATE_TIME, LocalDate::from);
    }

}
