package no.nav.melosys.domain.dokument.organisasjon;

import java.util.Arrays;

import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrganisasjonsDetaljerTest {

    private SemistrukturertAdresse adresse;

    private String linje1 = "LINJE1  ";
    private String linje2 = "LINJE2";
    private String linje3 = "LINJE3";
    private String postnr = "postnummer";
    private String kommunenr = "kommunenr";
    private String poststedUtland = "poststedUtland";

    @Before
    public void setUp() {

        Periode periode = mock(Periode.class);
        when(periode.erGyldig()).thenReturn(true);

        adresse = new SemistrukturertAdresse();
        adresse.setAdresselinje1(linje1);
        adresse.setAdresselinje2(linje2);
        adresse.setAdresselinje3(linje3);
        adresse.setPostnr(postnr);
        adresse.setPoststedUtland(poststedUtland);
        adresse.setKommunenr(kommunenr);
        adresse.setGyldighetsperiode(periode);
    }

    @Test
    public void testKonverterForretningsadresseTilStrukturertAdresse() {
        String landkode = "NO";
        adresse.setLandkode(landkode);

        OrganisasjonsDetaljer orgDetaljer = new OrganisasjonsDetaljer();
        orgDetaljer.forretningsadresse = Arrays.asList(adresse);

        StrukturertAdresse resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse();
        assertThat(resultatAdresse.gatenavn).isEqualTo(linje1.trim() + " " + linje2 + " " + linje3);
        assertThat(resultatAdresse.landKode).isEqualTo(landkode);

        // Har kun postnummer for norske registeradresses. Slåes opp med kodeverkservice ved behov
        assertThat(resultatAdresse.postnummer).isEqualTo(postnr);
        assertThat(resultatAdresse.poststed).isEmpty();
    }

    @Test
    public void testKonverterUtenlandskForretningsadresseTilStrukturertAdresse() {
        String landkode = "DK";
        adresse.setLandkode(landkode);

        OrganisasjonsDetaljer orgDetaljer = new OrganisasjonsDetaljer();
        orgDetaljer.forretningsadresse = Arrays.asList(adresse);

        StrukturertAdresse resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse();
        assertThat(resultatAdresse.gatenavn).isEqualTo(linje1.trim() + " " + linje2 + " " + linje3);
        assertThat(resultatAdresse.landKode).isEqualTo(landkode);

        // Har ikke utenlandsk postnummer, kun poststed
        assertThat(resultatAdresse.postnummer).isEmpty();
        assertThat(resultatAdresse.poststed).isEqualTo(poststedUtland);
    }

    @Test
    public void testNullAdresse() {
        OrganisasjonsDetaljer orgDetaljer = new OrganisasjonsDetaljer();
        StrukturertAdresse resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse();

        assertThat(resultatAdresse).isNull();
    }
}
