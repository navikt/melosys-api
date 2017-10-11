package no.nav.melosys.domain.dokument.jaxb;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeXmlAdapter extends AbstractDateXmlAdapter<LocalDateTime> {

    public OffsetDateTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_OFFSET_DATE_TIME, LocalDateTime::from);
    }

}
