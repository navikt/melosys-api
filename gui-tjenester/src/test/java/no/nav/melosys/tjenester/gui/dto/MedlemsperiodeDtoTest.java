package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.tjenester.gui.dto.util.DtoUtils;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MedlemsperiodeDtoTest {

    @Test
    public void tilLocalDate_onJodaTime_returnsJavaTime() {
        final org.joda.time.format.DateTimeFormatter jodaFormatter =
                org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-dd");
        final org.joda.time.LocalDate jodaLocalDate =
                org.joda.time.LocalDate.parse("2009-10-01", jodaFormatter);

        LocalDate localDate = DtoUtils.tilLocalDate(jodaLocalDate);

        assertNotNull(localDate);
        assertEquals(jodaLocalDate.getDayOfMonth(), localDate.getDayOfMonth());
        assertEquals(jodaLocalDate.getMonthOfYear(), localDate.getMonthValue());
        assertEquals(jodaLocalDate.getYear(), localDate.getYear());
    }
}
