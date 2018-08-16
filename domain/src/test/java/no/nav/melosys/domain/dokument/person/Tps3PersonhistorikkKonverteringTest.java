package no.nav.melosys.domain.dokument.person;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;

public class Tps3PersonhistorikkKonverteringTest {

    private static final String XML_PATH = "person/FJERNET_historikk.xml";

    private DokumentFactory factory;

    @Before
    public void setUp() {
        Jaxb2Marshaller marshaller = new JaxbConfig().jaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    public void testKonverteringPersonhistorikk() throws Exception {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(XML_PATH);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(kilde, Charset.forName("UTF-8")))) {
            Saksopplysning saksopplysning = new Saksopplysning();

            String xmlStr = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            saksopplysning.setDokumentXml(xmlStr);
            saksopplysning.setType(SaksopplysningType.PERSONHISTORIKK);
            saksopplysning.setVersjon("3.4");

            factory.lagDokument(saksopplysning);

            PersonhistorikkDokument dokument = (PersonhistorikkDokument) saksopplysning.getDokument();

            assertThat(dokument).isNotNull();
            assertThat(dokument.statsborgerskapListe).isNotNull();
            assertThat(dokument.bostedsadressePeriodeListe).isNotNull();
            assertThat(dokument.postadressePeriodeListe).isNotNull();
            assertThat(dokument.midlertidigAdressePeriodeListe).isNotNull();

            for (StatsborgerskapPeriode statsborgerskapPeriode : dokument.statsborgerskapListe) {
                assertThat(statsborgerskapPeriode.endringstidspunkt).isNotNull();
                assertThat(statsborgerskapPeriode.statsborgerskap).isNotNull();
            }

            for (BostedsadressePeriode bostedsadressePeriode : dokument.bostedsadressePeriodeListe) {
                assertThat(bostedsadressePeriode.endringstidspunkt).isNotNull();
                assertThat(bostedsadressePeriode.bostedsadresse.getLand()).isNotNull();
            }

            for (PostadressePeriode postadressePeriode : dokument.postadressePeriodeListe) {
                assertThat(postadressePeriode.endringstidspunkt).isNotNull();
                assertThat(postadressePeriode.postadresse.land).isNotNull();
            }

            for (MidlertidigPostadresse midlertidigPostadresse : dokument.midlertidigAdressePeriodeListe) {
                assertThat(midlertidigPostadresse.endringstidspunkt).isNotNull();
                assertThat(midlertidigPostadresse.land).isNotNull();
            }

        }
    }


}
