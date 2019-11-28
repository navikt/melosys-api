package no.nav.melosys.domain.dokument.person;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.KonverteringTest;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;

public class Tps3PersonhistorikkKonverteringTest implements KonverteringTest {

    private static final String XML_PATH = "person/person_historikk.xml";

    private DokumentFactory factory;

    @Before
    public void setUp() {
        Jaxb2Marshaller marshaller = JaxbConfig.jaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    public void testKonverteringPersonhistorikk() throws Exception {
        PersonhistorikkDokument dokument = (PersonhistorikkDokument) getSaksopplysning(XML_PATH).getDokument();

        assertThat(dokument).isNotNull();
        assertThat(dokument.statsborgerskapListe).isNotNull();
        assertThat(dokument.bostedsadressePeriodeListe).isNotNull();
        assertThat(dokument.postadressePeriodeListe).isNotNull();
        assertThat(dokument.midlertidigAdressePeriodeListe).isNotNull();

        for (StatsborgerskapPeriode statsborgerskapPeriode : dokument.statsborgerskapListe) {
            assertThat(statsborgerskapPeriode.endretAv).isNotNull();
            assertThat(statsborgerskapPeriode.endringstidspunkt).isNotNull();
            assertThat(statsborgerskapPeriode.statsborgerskap).isNotNull();
        }

        for (BostedsadressePeriode bostedsadressePeriode : dokument.bostedsadressePeriodeListe) {
            assertThat(bostedsadressePeriode.endringstidspunkt).isNotNull();
            assertThat(bostedsadressePeriode.periode).isNotNull();
            assertThat(bostedsadressePeriode.bostedsadresse.getLand()).isNotNull();
        }

        for (PostadressePeriode postadressePeriode : dokument.postadressePeriodeListe) {
            assertThat(postadressePeriode.endringstidspunkt).isNotNull();
            assertThat(postadressePeriode.periode).isNotNull();
            assertThat(postadressePeriode.postadresse.land).isNotNull();
        }

        for (MidlertidigPostadresse midlertidigPostadresse : dokument.midlertidigAdressePeriodeListe) {
            assertThat(midlertidigPostadresse.endringstidspunkt).isNotNull();
            assertThat(midlertidigPostadresse.postleveringsPeriode).isNotNull();
            assertThat(midlertidigPostadresse.land).isNotNull();
        }
    }

    @Override
    public Saksopplysning getSaksopplysning(String ressurs) throws IOException {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(ressurs);
        Objects.requireNonNull(kilde);
        return konverter(kilde, factory, SaksopplysningType.PERSHIST, "3.4");
    }
}
