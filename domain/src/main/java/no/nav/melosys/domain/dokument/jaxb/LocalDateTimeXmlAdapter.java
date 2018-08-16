package no.nav.melosys.domain.dokument.jaxb;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeXmlAdapter extends AbstractDateXmlAdapter<LocalDateTime> {

    public LocalDateTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_DATE_TIME, LocalDateTime::from);
    }
}
