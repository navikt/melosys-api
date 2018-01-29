package no.nav.melosys.integrasjon;

import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class KonverteringsUtilsTest {

    @Test
    public void localDateToXMLGregorianCalendar() throws Exception {
        final LocalDate date = LocalDate.now();
        XMLGregorianCalendar xmlDate = KonverteringsUtils.localDateTimeToXMLGregorianCalendar(date.atStartOfDay());
        assertNotNull(xmlDate);
        assertEquals(date.getYear(), xmlDate.getYear());
        assertEquals(date.getMonthValue(), xmlDate.getMonth());
        assertEquals(date.getDayOfMonth(), xmlDate.getDay());
    }

}
