package no.nav.melosys.domain.saksflyt;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.jpa.PropertiesConverter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Test;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ProsessinstansTest {

    @Test
    public void testDataString() {
        String s = "Problematisk streng med # og = skal tåles";
        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();
        pi1.setData(AVSENDER_NAVN, s);
        pi2.setData(new PropertiesConverter().convertToEntityAttribute(new PropertiesConverter().convertToDatabaseColumn(pi1.getData())));

        Assert.assertEquals(s, pi2.getData(AVSENDER_NAVN));
    }

    @Test
    public void testDataPeriode() {
        Periode fraNå = new Periode(LocalDate.now(), null);
        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();
        pi1.setData(SØKNADSPERIODE, fraNå);
        pi2.setData(new PropertiesConverter().convertToEntityAttribute(new PropertiesConverter().convertToDatabaseColumn(pi1.getData())));

        Assert.assertEquals(pi1.getData(SØKNADSPERIODE, Periode.class).getFom(), pi2.getData(SØKNADSPERIODE, Periode.class).getFom());
        Assert.assertEquals(pi1.getData(SØKNADSPERIODE, Periode.class).getTom(), pi2.getData(SØKNADSPERIODE, Periode.class).getTom());
    }

    @Test
    public void testDataList() {
        List<String> bareHoppeland = Collections.singletonList("HOP");
        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();
        pi1.setData(OPPHOLDSLAND, bareHoppeland);
        pi2.setData(new PropertiesConverter().convertToEntityAttribute(new PropertiesConverter().convertToDatabaseColumn(pi1.getData())));

        Assert.assertEquals(1, pi2.getData(OPPHOLDSLAND, List.class).size());
        Assert.assertEquals(pi1.getData(OPPHOLDSLAND, List.class).get(0), pi2.getData(OPPHOLDSLAND, List.class).get(0));
    }

    @Test
    public void leggTilHendelse() {
        final ProsessSteg steg = ProsessSteg.JFR_OPPRETT_SØKNAD;
        final String type = "Teknisk feil";
        final String melding = "Feilmelding";
        final Throwable t = new Throwable();
        Prosessinstans pi = new Prosessinstans();
        pi.setSteg(steg);
        pi.leggTilHendelse(type, melding, t);

        assertThat(pi.getHendelser()).isNotEmpty();
        ProsessinstansHendelse hendelse = pi.getHendelser().get(0);
        String forventetMelding = melding + " - " + ExceptionUtils.getStackTrace(t);
        ProsessinstansHendelse forventetHendelse = new ProsessinstansHendelse(pi, hendelse.getDato(), steg, type, forventetMelding);
        assertThat(hendelse).isEqualTo(forventetHendelse);
    }
}
