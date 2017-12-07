package no.nav.melosys.domain.dokument.jaxb;

import java.time.Year;
import java.time.format.DateTimeFormatter;

public class XMLYearToYearAdapter extends AbstractDateXmlAdapter<Year> {

    public XMLYearToYearAdapter() {
        super(DateTimeFormatter.ofPattern("yyyy"), Year::from);
    }
}
