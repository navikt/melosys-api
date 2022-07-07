package no.nav.melosys.service.dokument.brev.mapper.felles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrevMapperUtilsTest {

    @Test
    void convertToXMLGregorianCalendarRemoveTimezone() {
        System.out.println("ZoneId.systemDefault()=" + ZoneId.systemDefault());

        LocalDate localDate = LocalDate.parse("2019-04-01");
        Instant april_1 = localDate.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant();
        XMLGregorianCalendar xmlGregorianCalendar = BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone(april_1);
        assertThat(xmlGregorianCalendar.getDay()).isEqualTo(1);
    }
}
