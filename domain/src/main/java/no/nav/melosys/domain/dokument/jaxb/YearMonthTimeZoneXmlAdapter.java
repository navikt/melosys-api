package no.nav.melosys.domain.dokument.jaxb;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class YearMonthTimeZoneXmlAdapter extends AbstractDateXmlAdapter<YearMonth> {

    public YearMonthTimeZoneXmlAdapter() { super(DateTimeFormatter.ofPattern("yyyy-MMXXX"), YearMonth::from);}

}