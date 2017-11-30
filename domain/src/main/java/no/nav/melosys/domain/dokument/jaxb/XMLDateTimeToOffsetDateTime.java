package no.nav.melosys.domain.dokument.jaxb;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class XMLDateTimeToOffsetDateTime extends AbstractDateXmlAdapter<OffsetDateTime> {

    public XMLDateTimeToOffsetDateTime() {
        super(DateTimeFormatter.ISO_DATE_TIME, OffsetDateTime::from);
    }
}
