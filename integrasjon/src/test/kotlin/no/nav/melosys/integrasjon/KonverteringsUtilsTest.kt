package no.nav.melosys.integrasjon;

import java.time.LocalDate;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class KonverteringsUtilsTest {

    @Test
    public void localDateToXMLGregorianCalendar() throws Exception {
        final LocalDate date = LocalDate.now();
        XMLGregorianCalendar xmlDate = KonverteringsUtils.localDateTimeToXMLGregorianCalendar(date.atStartOfDay());
        assertThat(xmlDate).isNotNull();
        assertThat(date.getYear()).isEqualTo(xmlDate.getYear());
        assertThat(date.getMonthValue()).isEqualTo(xmlDate.getMonth());
        assertThat(date.getDayOfMonth()).isEqualTo(xmlDate.getDay());
    }
}
