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

    @Test
    public void javaDateToJodaDate() {
        final LocalDate dateBC = LocalDate.parse("-0001-01-01");
        final LocalDate dateAD = LocalDate.parse("0001-01-01");

        assertEquals(-1, dateBC.getYear());
        assertEquals(1, dateAD.getYear());

        org.joda.time.LocalDate jodaDateBC = KonverteringsUtils.javaLocalDateToJodaLocalDate(dateBC);

        assertNotNull(jodaDateBC);
        assertEquals(dateBC.getYear(), jodaDateBC.getYear());
        assertEquals(dateBC.getMonthValue(), jodaDateBC.getMonthOfYear());
        assertEquals(dateBC.getDayOfMonth(), jodaDateBC.getDayOfMonth());

        org.joda.time.LocalDate jodaDateAD = KonverteringsUtils.javaLocalDateToJodaLocalDate(dateAD);

        assertNotNull(jodaDateAD);
        assertEquals(dateAD.getYear(), jodaDateAD.getYear());
        assertEquals(dateAD.getMonthValue(), jodaDateAD.getMonthOfYear());
        assertEquals(dateAD.getDayOfMonth(), jodaDateAD.getDayOfMonth());
    }

    @Test
    public void jodaDateToJavaDate() {
        final org.joda.time.LocalDate dateBC = org.joda.time.LocalDate.parse("-0001-01-01");
        final org.joda.time.LocalDate dateAD = org.joda.time.LocalDate.parse("0001-01-01");

        assertEquals(-1, dateBC.getYear());
        assertEquals(1, dateAD.getYear());

        LocalDate javaDateBC = KonverteringsUtils.jodaLocalDateToJavaLocalDate(dateBC);

        assertNotNull(javaDateBC);
        assertEquals(dateBC.getYear(), javaDateBC.getYear());
        assertEquals(dateBC.getMonthOfYear(), javaDateBC.getMonthValue());
        assertEquals(dateBC.getDayOfMonth(), javaDateBC.getDayOfMonth());

        LocalDate javaDateAD = KonverteringsUtils.jodaLocalDateToJavaLocalDate(dateAD);

        assertNotNull(javaDateAD);
        assertEquals(dateAD.getYear(), javaDateAD.getYear());
        assertEquals(dateAD.getMonthOfYear(), javaDateAD.getMonthValue());
        assertEquals(dateAD.getDayOfMonth(), javaDateAD.getDayOfMonth());
    }

}
