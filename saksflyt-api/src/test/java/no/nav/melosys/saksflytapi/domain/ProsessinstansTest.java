package no.nav.melosys.saksflytapi.domain;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.jpa.PropertiesConverter;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.saksflytapi.domain.ProsessDataKey.*;
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
    void testDataString2() throws IOException {
        Prosessinstans pi1 = new Prosessinstans();
        Properties properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream("procdata.properties"));
        pi1.setData(properties);

        assertThat(pi1.getData(AVSENDER_ID, String.class)).isEqualTo("myFnr!");
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
    void testDataList() {
        List<String> bareHoppeland = Collections.singletonList("HOP");
        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();
        pi1.setData(OPPHOLDSLAND, bareHoppeland);
        pi2.setData(new PropertiesConverter().convertToEntityAttribute(new PropertiesConverter().convertToDatabaseColumn(pi1.getData())));

        assertThat(pi2.getData(OPPHOLDSLAND, List.class)).hasSize(1);
        assertThat(pi1.getData(OPPHOLDSLAND, List.class).get(0)).isEqualTo(pi2.getData(OPPHOLDSLAND, List.class).get(0));
    }

    @Test
    void testBrevbestilling() {
        MangelbrevBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.MANGELBREV_BRUKER)
            .medBestillKopi(true)
            .medBestillUtkast(true)
            .build();

        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();

        pi1.setData(BREVBESTILLING, brevbestilling);
        pi2.setData(new PropertiesConverter().convertToEntityAttribute(new PropertiesConverter().convertToDatabaseColumn(pi1.getData())));

        DokgenBrevbestilling data = pi2.getData(BREVBESTILLING, DokgenBrevbestilling.class);
        assertThat(data).isInstanceOf(MangelbrevBrevbestilling.class);
        assertThat(data.getProduserbartdokument()).isEqualTo(Produserbaredokumenter.MANGELBREV_BRUKER);
        assertThat(data.isBestillKopi()).isTrue();
        assertThat(data.isBestillUtkast()).isTrue();
    }
}
