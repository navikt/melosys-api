package no.nav.melosys.domain.dokument.organisasjon;

import java.util.Arrays;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrganisasjonsDetaljerTest {

    private SemistrukturertAdresse adresse;

    private String linje1 = "LINJE1  ";
    private String linje2 = "LINJE2";
    private String linje3 = "LINJE3";
    private String postnr = "postnummer";
    private String poststed = "poststed";
    private String poststedUtland = "poststedUtland";

    @BeforeEach
    public void setUp() {

        Periode periode = mock(Periode.class);
        when(periode.erGyldig()).thenReturn(true);

        adresse = new SemistrukturertAdresse();
        adresse.setAdresselinje1(linje1);
        adresse.setAdresselinje2(linje2);
        adresse.setAdresselinje3(linje3);
        adresse.setPostnr(postnr);
        adresse.setPoststed(poststed);
        adresse.setPoststedUtland(poststedUtland);
        String kommunenr = "kommunenr";
        adresse.setKommunenr(kommunenr);
        adresse.setGyldighetsperiode(periode);
    }

    @Test
    public void testKonverterForretningsadresseTilUstrukturertAdresse() {
        String landkode = "NO";
        adresse.setLandkode(landkode);

        OrganisasjonsDetaljer orgDetaljer = new OrganisasjonsDetaljer();
        orgDetaljer.forretningsadresse = Arrays.asList(adresse);

        UstrukturertAdresse resultatAdresse = orgDetaljer.hentUstrukturertForretningsadresse();
        assertThat(resultatAdresse.getAdresselinje(1)).isEqualTo(linje1);
        assertThat(resultatAdresse.getAdresselinje(2)).isEqualTo(linje2);
        assertThat(resultatAdresse.getAdresselinje(3)).isEqualTo(linje3);
        assertThat(resultatAdresse.getAdresselinje(4)).isEqualTo(postnr + " " + poststed);
        assertThat(resultatAdresse.landkode).isEqualTo(landkode);
    }

    @Test
    public void testKonverterUtenlandskForretningsadresseTilUstrukturertAdresse() {
        String landkode = "DK";
        adresse.setLandkode(landkode);

        OrganisasjonsDetaljer orgDetaljer = new OrganisasjonsDetaljer();
        orgDetaljer.forretningsadresse = Arrays.asList(adresse);

        UstrukturertAdresse resultatAdresse = orgDetaljer.hentUstrukturertForretningsadresse();
        assertThat(resultatAdresse.getAdresselinje(1)).isEqualTo(linje1);
        assertThat(resultatAdresse.getAdresselinje(2)).isEqualTo(linje2);
        assertThat(resultatAdresse.getAdresselinje(3)).isEqualTo(linje3);
        assertThat(resultatAdresse.getAdresselinje(4)).isEqualTo(poststedUtland);
        assertThat(resultatAdresse.landkode).isEqualTo(landkode);
    }

    @Test
    public void testKonverterForretningsadresseTilStrukturertAdresse() {
        String landkode = "NO";
        adresse.setLandkode(landkode);

        OrganisasjonsDetaljer orgDetaljer = new OrganisasjonsDetaljer();
        orgDetaljer.forretningsadresse = Arrays.asList(adresse);

        StrukturertAdresse resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse();
        assertThat(resultatAdresse.gatenavn).isEqualTo(linje1.trim() + " " + linje2 + " " + linje3);
        assertThat(resultatAdresse.landkode).isEqualTo(landkode);

        assertThat(resultatAdresse.postnummer).isEqualTo(postnr);
        // Ikke alltid poststed for norske registeradresser. Slåes opp med kodeverkservice ved behov
        assertThat(resultatAdresse.poststed).isEqualTo(poststed);
    }

    @Test
    public void testKonverterUtenlandskForretningsadresseTilStrukturertAdresse() {
        String landkode = "DK";
        adresse.setLandkode(landkode);

        OrganisasjonsDetaljer orgDetaljer = new OrganisasjonsDetaljer();
        orgDetaljer.forretningsadresse = Arrays.asList(adresse);

        StrukturertAdresse resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse();
        assertThat(resultatAdresse.gatenavn).isEqualTo(linje1.trim() + " " + linje2 + " " + linje3);
        assertThat(resultatAdresse.landkode).isEqualTo(landkode);
        assertThat(resultatAdresse.postnummer).isEqualTo(postnr);
        assertThat(resultatAdresse.poststed).isEqualTo(poststedUtland);
    }

    @Test
    public void testNullAdresse() {
        OrganisasjonsDetaljer orgDetaljer = new OrganisasjonsDetaljer();
        StrukturertAdresse resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse();

        assertThat(resultatAdresse).isNull();
    }
}
