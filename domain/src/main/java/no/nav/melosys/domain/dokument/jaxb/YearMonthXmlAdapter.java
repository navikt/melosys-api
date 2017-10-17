package no.nav.melosys.domain.dokument.jaxb;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class YearMonthXmlAdapter extends AbstractDateXmlAdapter<YearMonth> {

    public YearMonthXmlAdapter() { super(DateTimeFormatter.ofPattern("yyyy-MM"), YearMonth::from);}

}
