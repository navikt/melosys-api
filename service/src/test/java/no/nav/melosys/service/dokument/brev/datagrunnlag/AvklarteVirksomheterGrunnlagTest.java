package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagForetakUtland;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagNorskVirksomhet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklarteVirksomheterGrunnlagTest {

    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;

    @Mock
    private KodeverkService kodeverkService;

    private AvklarteVirksomheterGrunnlag dataGrunnlag;

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet arbeidsgiver = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(arbeidsgiver));

        when(kodeverkService.dekod(any(FellesKodeverk.class), anyString(), any(LocalDate.class))).thenReturn("Poststed");

        dataGrunnlag = new AvklarteVirksomheterGrunnlag(mock(Behandling.class), avklarteVirksomheterService, kodeverkService);
    }

    @Test
    public void hentAlleNorskeVirksomheter_foreventerEnVirksomhet() throws IkkeFunnetException, TekniskException {
        Collection<AvklartVirksomhet> norskeVirksomheter = dataGrunnlag.hentAlleNorskeVirksomheterMedAdresse();
        assertThat(norskeVirksomheter).hasSize(1);
        dataGrunnlag.hentAlleNorskeVirksomheterMedAdresse();
        verify(avklarteVirksomheterService, times(1)).hentAlleNorskeVirksomheter(any(), any());
    }

    @Test
    public void hentUtenlandskeArbeidsgivere_medUtenlandskArbeidsgiverOgSelvstendig_henterKunArbeidsgivere() {
        AvklartVirksomhet utenlandskSelvstendigForetak = new AvklartVirksomhet(lagForetakUtland(true));
        AvklartVirksomhet utenlandskArbeidsgiver = new AvklartVirksomhet(lagForetakUtland(false));

        List<AvklartVirksomhet> utenlandskeVirksomheter = Arrays.asList(utenlandskSelvstendigForetak, utenlandskArbeidsgiver);
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(utenlandskeVirksomheter);

        List<AvklartVirksomhet> utenlandskeArbeidsgivere = dataGrunnlag.hentUtenlandskeArbeidsgivere();
        assertThat(utenlandskeArbeidsgivere).containsExactly(utenlandskArbeidsgiver);
    }

    @Test
    public void hentUtenlandskeSelvstendige_medUtenlandskArbeidsgiverOgSelvstendig_henterKunSelvstendige() {
        AvklartVirksomhet utenlandskSelvstendigForetak = new AvklartVirksomhet(lagForetakUtland(true));
        AvklartVirksomhet utenlandskArbeidsgiver = new AvklartVirksomhet(lagForetakUtland(false));

        List<AvklartVirksomhet> utenlandskeVirksomheter = Arrays.asList(utenlandskSelvstendigForetak, utenlandskArbeidsgiver);
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(utenlandskeVirksomheter);

        List<AvklartVirksomhet> utenlandskeSelvstendige = dataGrunnlag.hentUtenlandskeSelvstendige();
        assertThat(utenlandskeSelvstendige).containsExactly(utenlandskSelvstendigForetak);
    }

    @Test
    public void hentHovedvirksomhet_medEnNorskVirksomhet_girNorskHovedvirksomhet() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        AvklartVirksomhet avklartVirksomhet = dataGrunnlag.hentHovedvirksomhet();
        assertThat(avklartVirksomhet).isEqualTo(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medNorskOgUtenlandskVirksomhet_girNorskHovedvirksomhet() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        AvklartVirksomhet utenlandskAvklartVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(utenlandskAvklartVirksomhet));

        AvklartVirksomhet hovedvirksomhet = dataGrunnlag.hentHovedvirksomhet();
        dataGrunnlag.hentBivirksomheter();
        assertThat(hovedvirksomhet).isEqualTo(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medKunUtenlandskVirksomhet_girUtenlandskVirksomhet() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.emptyList());

        AvklartVirksomhet hovedvirksomhet = dataGrunnlag.hentHovedvirksomhet();
        assertThat(hovedvirksomhet).isEqualToComparingFieldByField(forventetUtenlandskVirksomhet);
    }

    @Test
    public void hentBivirksomheter_medEnNorskVirksomhet_girIngenBivirksomheter() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).isEmpty();
    }

    @Test
    public void hentBivirksomheter_medEnUtenlandskVirksomhet_girIngenBivirksomheter() throws IkkeFunnetException, TekniskException {
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.emptyList());

        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).isEmpty();
    }

    @Test
    public void hentBivirksomheter_medToNorskeVirksomheter_girEnNorskBivirksomhet() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Arrays.asList(norskVirksomhet, norskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).containsExactly(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medNorskOgUtenlandskVirksomhet_girUtenlandskBivirksomhet() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));

        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).hasSize(1);

        assertThat(bivirksomheter.iterator().next()).isEqualToComparingFieldByField(forventetUtenlandskVirksomhet);
    }

    @Test
    public void utfyllManglendeAdressefelter_gyldigForretningsadresse_girForretningsadresse() {
        StrukturertAdresse adresse = dataGrunnlag.utfyllManglendeAdressefelter(lagOrganisasjonDokument("2345", "Forretningsgatenavn"));

        assertThat(adresse.gatenavn).isEqualTo("Forretningsgatenavn");
        assertThat(adresse.postnummer).isEqualTo("2345");
        assertThat(adresse.poststed).isEqualTo("Poststed");
        assertThat(adresse.landkode).isEqualTo("NO");

        verify(kodeverkService).dekod(eq(FellesKodeverk.POSTNUMMER), eq("2345"), any(LocalDate.class));
    }

    @Test
    public void utfyllManglendeAdressefelter_forretningsadresseManglerGatenavn_girForretningsadresseMedBlanktGatenavn() {
        StrukturertAdresse adresse = dataGrunnlag.utfyllManglendeAdressefelter(lagOrganisasjonDokument("2345", null));

        assertThat(adresse.gatenavn).isEqualTo(" ");
        assertThat(adresse.postnummer).isEqualTo("2345");
        assertThat(adresse.poststed).isEqualTo("Poststed");
        assertThat(adresse.landkode).isEqualTo("NO");

        verify(kodeverkService).dekod(eq(FellesKodeverk.POSTNUMMER), eq("2345"), any(LocalDate.class));
    }

    @Test
    public void utfyllManglendeAdressefelter_utenladsnkIngenForretningsadressePostadresseUtenPostnummer_postnummerTomString() {
        var organisasjonDokument = lagOrganisasjonDokument(null, null, null, "DK");
        organisasjonDokument.organisasjonDetaljer.forretningsadresse = Collections.emptyList();
        organisasjonDokument.organisasjonDetaljer.postadresse.stream().findFirst().ifPresent(a -> ((SemistrukturertAdresse)a).setPostnr(null));
        StrukturertAdresse adresse = dataGrunnlag.utfyllManglendeAdressefelter(organisasjonDokument);

        assertThat(adresse.gatenavn).isEqualTo("Postgatenavn");
        assertThat(adresse.postnummer).isEqualTo(" ");
        assertThat(adresse.poststed).isEqualTo("Postpoststed");
        assertThat(adresse.landkode).isEqualTo("DK");

        verify(kodeverkService, never()).dekod(any(), any(), any());
    }

    @Test
    public void utfyllManglendeAdressefelter_forretningsadresseManglerPostnr_girPostadresse() {
        StrukturertAdresse adresse = dataGrunnlag.utfyllManglendeAdressefelter(lagOrganisasjonDokument(null, null));

        assertThat(adresse.gatenavn).isEqualTo("Postgatenavn");
        assertThat(adresse.postnummer).isEqualTo("6789");
        assertThat(adresse.poststed).isEqualTo("Poststed");
        assertThat(adresse.landkode).isEqualTo("NO");

        verify(kodeverkService).dekod(eq(FellesKodeverk.POSTNUMMER), eq("6789"), any(LocalDate.class));
    }

    private OrganisasjonDokument lagOrganisasjonDokument(String forretningsPostnr, String forretningsGatenavn) {
        return lagOrganisasjonDokument(forretningsPostnr, forretningsGatenavn, "6789", "NO");
    }

    private OrganisasjonDokument lagOrganisasjonDokument(String forretningsPostnr, String forretningsGatenavn, String postadressePostnr, String postadresseLand) {
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        OrganisasjonsDetaljer organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonDokument.setOrganisasjonDetaljer(organisasjonsDetaljer);
        SemistrukturertAdresse forretningsadresse = new SemistrukturertAdresse();
        organisasjonsDetaljer.forretningsadresse.add(forretningsadresse);
        forretningsadresse.setAdresselinje1(forretningsGatenavn);
        forretningsadresse.setPostnr(forretningsPostnr);
        forretningsadresse.setPoststed("Forretningspoststed");
        forretningsadresse.setLandkode("NO");
        forretningsadresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
        SemistrukturertAdresse postadresse = new SemistrukturertAdresse();
        organisasjonsDetaljer.postadresse.add(postadresse);
        postadresse.setAdresselinje1("Postgatenavn");
        postadresse.setPostnr(postadressePostnr);
        postadresse.setPoststed("Postpoststed");
        postadresse.setLandkode(postadresseLand);
        postadresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

        return organisasjonDokument;
    }
}
