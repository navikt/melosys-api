package no.nav.melosys.domain.dokument;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Epost;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Telefonnummer;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;

class DokumentFactoryTest {

    DokumentFactory factory;

    @BeforeEach
    void setUp() {
        Jaxb2Marshaller marshaller = JaxbConfig.getJaxb2Marshaller();

        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    void lagDokument() throws Exception {
        Saksopplysning test = new Saksopplysning();

        InputStream kilde = getClass().getClassLoader().getResourceAsStream("arbeidsforhold/99999999995.xml");
        StringBuilder stringBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (kilde, StandardCharsets.UTF_8))) {
            int c;
            while ((c = reader.read()) != -1) {
                stringBuilder.append((char) c);
            }
        }
        test.leggTilKildesystemOgMottattDokument(null, stringBuilder.toString());

        test.setType(SaksopplysningType.ARBFORH);
        test.setVersjon("3.0");

        factory.lagDokument(test);

        SaksopplysningDokument dokument = test.getDokument();
        assertThat(dokument).isInstanceOf(ArbeidsforholdDokument.class);
        assertThat(((ArbeidsforholdDokument) dokument).getArbeidsforhold().get(25).getUtenlandsopphold()).isNotEmpty();

    }

    @Test
    void lagOrganisasjonDokument() throws Exception {
        Saksopplysning test = new Saksopplysning();

        InputStream kilde = getClass().getClassLoader().getResourceAsStream("organisasjon/974652366.xml");
        StringBuilder stringBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (kilde, StandardCharsets.UTF_8))) {
            int c;
            while ((c = reader.read()) != -1) {
                stringBuilder.append((char) c);
            }
        }
        test.leggTilKildesystemOgMottattDokument(null, stringBuilder.toString());

        test.setType(SaksopplysningType.ORG);
        test.setVersjon("4.0");

        factory.lagDokument(test);

        SaksopplysningDokument dokument = test.getDokument();
        assertThat(dokument).isInstanceOf(OrganisasjonDokument.class);

        OrganisasjonDokument organisasjonDokument = (OrganisasjonDokument) dokument;

        GeografiskAdresse geografiskAdresse = organisasjonDokument.getOrganisasjonDetaljer().postadresse.get(0);
        assertThat(geografiskAdresse).isInstanceOf(SemistrukturertAdresse.class);
        assertThat(((SemistrukturertAdresse) geografiskAdresse).getAdresselinje1()).isEqualTo("Postboks 7030");

        Epost epost = organisasjonDokument.getOrganisasjonDetaljer().getEpostadresse().get(0);
        assertThat(epost).isInstanceOf(Epost.class);
        assertThat(epost.getIdentifikator()).isEqualTo("post@hib.no");

        Telefonnummer telefon = organisasjonDokument.getOrganisasjonDetaljer().getTelefon().get(0);
        assertThat(telefon).isInstanceOf(Telefonnummer.class);
        assertThat(telefon.getIdentifikator()).isEqualTo("55 58 75 00");

        LocalDate opphoersdato = organisasjonDokument.getOrganisasjonDetaljer().getOpphoersdato();
        assertThat(opphoersdato).isInstanceOf(LocalDate.class);
        assertThat(opphoersdato).isEqualTo(LocalDate.of(2019, 12, 8));
    }

    @Test
    void lagSedDokument_xmlBlirProdusert() {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setStatsborgerskapKoder(Collections.singletonList("NO"));
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now()));
        sedDokument.setLovvalgBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        sedDokument.setErEndring(false);
        sedDokument.setRinaDokumentID("123");
        sedDokument.setRinaSaksnummer("saksnummer123");
        sedDokument.setFnr("333");
        sedDokument.setLovvalgslandKode(Landkoder.DE);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(sedDokument);

        String xml = factory.lagForenkletXml(saksopplysning);

        saksopplysning.leggTilKildesystemOgMottattDokument(null, xml);
        SaksopplysningDokument saksopplysningDokument = factory.lagDokument(saksopplysning);

        assertThat(xml).isNotNull();
        assertThat(saksopplysningDokument).isNotNull();


    }

}
