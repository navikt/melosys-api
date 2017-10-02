package no.nav.melosys.domain.dokument.jaxb;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public abstract class AbstractDateXmlAdapter<T extends TemporalAccessor> extends XmlAdapter<String, T> {

    private final DateTimeFormatter formatter;

    private final TemporalQuery<? extends T> temporalQuery;

    public AbstractDateXmlAdapter(DateTimeFormatter formatter, TemporalQuery<? extends T>  temporalQuery) {
        this.formatter = formatter;
        this.temporalQuery = temporalQuery;
    }

    @Override
    public T unmarshal(String stringValue) {
        return (stringValue  != null && stringValue.length() > 0) ? formatter.parse(stringValue, temporalQuery) : null;
    }

    @Override
    public String marshal(T value) {
        return value != null ? formatter.format(value) : null;
    }

}
