package no.nav.melosys.domain.dokument.jaxb;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalQuery;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LocalDateXmlAdapter extends XmlAdapter<String, LocalDate> {

    private final DateTimeFormatter formatter;

    private final TemporalQuery<? extends LocalDate> temporalQuery;

    public LocalDateXmlAdapter() {
        this.formatter = DateTimeFormatter.ISO_DATE;
        this.temporalQuery = LocalDate::from;
    }

    @Override
    public LocalDate unmarshal(String stringValue) {
        return stringValue != null ? formatter.parse(stringValue, temporalQuery) : null;
    }

    @Override
    public String marshal(LocalDate value) {
        return value != null ? formatter.format(value) : null;
    }
}
