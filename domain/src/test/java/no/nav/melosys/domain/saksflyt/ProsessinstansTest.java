package no.nav.melosys.domain.saksflyt;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.jpa.PropertiesConverter;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static org.assertj.core.api.Assertions.assertThat;

class ProsessinstansTest {

    @Test
    void testDataString() {
        String s = "Problematisk streng med # og = skal tåles";
        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();
        pi1.setData(AVSENDER_NAVN, s);
        pi2.setData(new PropertiesConverter().convertToEntityAttribute(new PropertiesConverter().convertToDatabaseColumn(pi1.getData())));

        assertThat(pi2.getData(AVSENDER_NAVN)).isEqualTo(s);
    }

    @Test
    void testDataPeriode() {
        Periode fraNå = new Periode(LocalDate.now(), null);
        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();
        pi1.setData(SØKNADSPERIODE, fraNå);
        pi2.setData(new PropertiesConverter().convertToEntityAttribute(new PropertiesConverter().convertToDatabaseColumn(pi1.getData())));


        assertThat(pi2.getData(SØKNADSPERIODE, Periode.class).getFom()).isEqualTo(pi1.getData(SØKNADSPERIODE, Periode.class).getFom());
        assertThat(pi2.getData(SØKNADSPERIODE, Periode.class).getTom()).isEqualTo(pi1.getData(SØKNADSPERIODE, Periode.class).getTom());
    }

    @Test
    public void testDataList() {
        List<String> bareHoppeland = Collections.singletonList("HOP");
        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();
        pi1.setData(OPPHOLDSLAND, bareHoppeland);
        pi2.setData(new PropertiesConverter().convertToEntityAttribute(new PropertiesConverter().convertToDatabaseColumn(pi1.getData())));

        assertThat(pi2.getData(OPPHOLDSLAND, List.class)).hasSize(1);
        assertThat(pi1.getData(OPPHOLDSLAND, List.class).get(0)).isEqualTo(pi2.getData(OPPHOLDSLAND, List.class).get(0));
    }
}
