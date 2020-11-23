package no.nav.melosys.domain.dokument.person;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.KonverteringTest;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// TODO: Fjernes sammen med xslt og testressurser
public class Tps3PersonKonverteringTest implements KonverteringTest {
    private static final String TPS_PERSON_3_0_MOCK = "person/tps_person_3.0_mock.xml";
    private static final String TPS_PERSON_MED_FAMILIERELASJONER = "person/familierelasjoner.xml";

    private static DokumentFactory factory;

    @BeforeClass
    public static void setUp() {
        Jaxb2Marshaller marshaller = JaxbConfig.jaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    public void testTpsTilDomene() throws Exception {
        Saksopplysning test = getSaksopplysning(TPS_PERSON_3_0_MOCK);

        PersonDokument p2 = (PersonDokument) test.getDokument();
        // Verifiser...
        assertEquals("SPFO", p2.diskresjonskode.getKode());
        assertTrue(p2.diskresjonskode.erKode7());
        assertEquals(LocalDate.of(2011, 11, 11), p2.fødselsdato);
        assertThat(p2.bostedsadresse.getGateadresse().getGatenummer()).isNull(); // tomt felt skal blir null, ikke 0
        assertThat(p2.bostedsadresse.getGateadresse().getHusnummer()).isEqualTo(7);
    }

    @Test
    public void testFamilierelasjoner() throws Exception {
        Saksopplysning test = getSaksopplysning(TPS_PERSON_MED_FAMILIERELASJONER);

        PersonDokument dokument = (PersonDokument) test.getDokument();
        // Verifiser...
        assertThat(dokument).isNotNull();
        assertThat(dokument.familiemedlemmer).isNotEmpty();
    }

    @Test
    public void testMidlertidigPostadresseUtland() throws Exception {
        final String kilde = "person/midlertidig_postadresse_utland.xml";
        PersonDokument dokument = (PersonDokument) getSaksopplysning(kilde).getDokument();

        assertThat(dokument).isNotNull();
        assertThat(dokument.postadresse).isNotNull();
        assertThat(dokument.midlertidigPostadresse).isNotNull();
        assertThat(dokument.midlertidigPostadresse.land.getKode()).isEqualTo("GBR");
    }

    @Test
    public void testMidlertidigPostadresseNorge() throws Exception {
        final String kilde = "person/midlertidig_postadresse_norge.xml";
        PersonDokument dokument = (PersonDokument) getSaksopplysning(kilde).getDokument();

        assertThat(dokument).isNotNull();
        assertThat(dokument.midlertidigPostadresse).isNotNull();

        MidlertidigPostadresseNorge midlertidigPostadresseNorge = (MidlertidigPostadresseNorge) dokument.midlertidigPostadresse;
        assertThat(midlertidigPostadresseNorge.gateadresse.getGatenummer()).isEqualTo(29);
        assertThat(midlertidigPostadresseNorge.gateadresse.getHusnummer()).isEqualTo(7);
    }

    @Override
    public Saksopplysning getSaksopplysning(String ressurs) throws IOException {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(ressurs);
        Objects.requireNonNull(kilde);
        return konverter(kilde, factory, SaksopplysningType.PERSOPL, "3.0");
    }
}
