package no.nav.melosys.domain.dokument.jaxb;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateXmlAdapter extends AbstractDateXmlAdapter<LocalDate> {

    public LocalDateXmlAdapter() {
        super(DateTimeFormatter.ISO_DATE, LocalDate::from);
    }

}
