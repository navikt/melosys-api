package no.nav.melosys.service.avklartefakta;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.SelvstendigForetak;
import no.nav.melosys.domain.dokument.adresse.Adresse;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VIRKSOMHET;
import static no.nav.melosys.service.BehandlingsgrunnlagStub.lagBehandlingsgrunnlag;
import static no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysninger;
import static no.nav.melosys.service.SaksopplysningStubs.lagOrganisasjonDokumenter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklarteVirksomheterServiceTest {

    @Mock
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private RegisterOppslagService registerOppslagService;

    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    @Mock
    private KodeverkService mockKodeverkService;

    @Mock
    private BehandlingService behandlingService;

    private Behandling behandling;

    private AvklarteVirksomheterService avklarteVirksomheterService;

    private String orgnr1 = "111111111";
    private String orgnr2 = "222222222";
    private String orgnr3 = "333333333";
    private String orgnr4 = "444444444";
    private String uuid1 = "a2k2jf-a3khs";
    private String uuid2 = "0dkf93-kj701";

    Function<OrganisasjonDokument, Adresse> ingenAdresse = org -> null;

    @Before
    public void setUp() {
        behandling = new Behandling();
        behandling.setId(1L);
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(new HashSet<>(Arrays.asList(orgnr1, uuid1)));

        when(mockKodeverkService.dekod(any(FellesKodeverk.class), anyString(), any(LocalDate.class))).thenReturn("Poststed");

        avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService, behandlingService, mockKodeverkService);
    }

    @Test
    public void hentUtenlandskeVirksomheter_girListeMedKunAvklarteForetak() throws TekniskException {
        ForetakUtland foretak1 = lagForetakUtland("Utland1", uuid1, null);
        ForetakUtland foretak2 = lagForetakUtland("Utland2", uuid2, "SE-123456789");
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag(Collections.emptyList(), Arrays.asList(foretak1, foretak2), Collections.emptyList()));

        List<AvklartVirksomhet> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling);
        assertThat(avklarteSelvstendigeOrgnumre.stream().map(av -> av.navn)).containsOnly("Utland1");
    }

    @Test
    public void hentUtenlandskeVirksomheter_girListeAvklartVirksomhetMedOrgnrIkkeUuid() throws TekniskException {
        ForetakUtland foretak1 = lagForetakUtland("Utland1", uuid1, "SE-123456789");
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag(Collections.emptyList(), Collections.singletonList(foretak1), Collections.emptyList()));

        List<AvklartVirksomhet> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling);
        assertThat(avklarteSelvstendigeOrgnumre.stream().map(av -> av.orgnr)).containsOnly("SE-123456789");
    }

    private ForetakUtland lagForetakUtland(String navn, String uuid, String orgnr) {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.navn = navn;
        foretakUtland.uuid = uuid;
        foretakUtland.orgnr = orgnr;
        return foretakUtland;
    }

    @Test
    public void hentSelvstendigeForetakOrgnumre_girListeMedKunAvklarteOrgnumre() throws TekniskException {
        List<String> selvstendigeForetak = Arrays.asList(orgnr1, orgnr2);
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag(selvstendigeForetak, Collections.emptyList(), Collections.emptyList()));

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeSelvstendigeForetakOrgnumre(behandling);
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(orgnr1);
    }

    @Test
    public void hentArbeidsgivendeEkstraOrgnumre_girListeMedKunAvklarteOrgnumre() throws TekniskException {
        List<String> arbeidgivendeEkstraOrgnumre = Arrays.asList(orgnr2, orgnr1);
        Set<Saksopplysning> saksopplysninger =
            lagArbeidsforholdOpplysninger(Collections.emptyList());
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag(Collections.emptyList(),Collections.emptyList(), arbeidgivendeEkstraOrgnumre));

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling);
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(orgnr1);
    }

    @Test
    public void hentArbeidsgivendeRegistreOrgnumre_girListeMedKunAvklarteOrgnumre() throws TekniskException {
        List<String> arbeidgivendeOrgnumreEkstra = Arrays.asList(orgnr1, orgnr2, orgnr3);
        Set<Saksopplysning> saksopplysninger =
            lagArbeidsforholdOpplysninger(arbeidgivendeOrgnumreEkstra);
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag(Collections.emptyList(),Collections.emptyList(), Collections.emptyList()));

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling);
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(orgnr1);
    }

    @Test
    public void testHentAvklarteNorskeForetak_girAvklarteArbeidsgivere() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        List<String> arbeidsgivereEkstra = Collections.singletonList(orgnr2);
        List<String> arbeidsgivereRegister = Collections.singletonList(orgnr3);

        Set<Saksopplysning> saksopplysninger =
            lagArbeidsforholdOpplysninger(arbeidsgivereRegister);

        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag(Collections.emptyList(), Collections.emptyList(), arbeidsgivereEkstra));

        Set<String> avklarteOrganisasjoner = new HashSet<>(Arrays.asList(orgnr2, orgnr3));
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(avklarteOrganisasjoner);

        leggTilIRegisterOppslag(Arrays.asList(orgnr2, orgnr3));

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService, behandlingService, mockKodeverkService);
        assertThat(avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, ingenAdresse).stream()
            .map(nv -> nv.orgnr)
            .collect(Collectors.toList())).contains(orgnr2, orgnr3);
    }

    @Test
    public void testHentAvklarteNorskeForetak_girAvklarteSelvstendigeForetak() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        List<String> selvstendigeForetak = Collections.singletonList(orgnr1);

        Set<Saksopplysning> saksopplysninger =
            lagArbeidsforholdOpplysninger(Collections.emptyList());

        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag(selvstendigeForetak, Collections.emptyList(), Collections.emptyList()));

        Set<String> avklarteOrganisasjoner = new HashSet<>(selvstendigeForetak);
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(avklarteOrganisasjoner);

        leggTilIRegisterOppslag(selvstendigeForetak);

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService, behandlingService, mockKodeverkService);
        assertThat(avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, ingenAdresse).stream()
            .map(nv -> nv.orgnr)
            .collect(Collectors.toList())).contains(orgnr1);
    }

    @Test
    public void lagreVirksomheterSomAvklartefakta_virksomhetErForetakUtland_valideringOKOgVirksomhetLagret() throws FunksjonellException, TekniskException {
        List<String> virksomhetIDer = List.of(uuid1);
        forberedValidering();

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(virksomhetIDer, 1L);
        verify(avklartefaktaService, times(1)).leggTilAvklarteFakta(1L, VIRKSOMHET, VIRKSOMHET.getKode(), uuid1, Avklartefakta.VALGT_FAKTA);
    }

    @Test
    public void lagreVirksomheterSomAvklartefakta_virksomhetErSelvstendigForetak_valideringOKOgVirksomhetLagret() throws FunksjonellException, TekniskException {
        List<String> virksomhetIDer = List.of(orgnr1);
        forberedValidering();

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(virksomhetIDer, 1L);
        verify(avklartefaktaService, times(1)).leggTilAvklarteFakta(1L, VIRKSOMHET, VIRKSOMHET.getKode(), orgnr1, Avklartefakta.VALGT_FAKTA);
    }

    @Test
    public void lagreVirksomheterSomAvklartefakta_virksomhetErLagtInnManuelt_valideringOKOgVirksomhetLagret() throws FunksjonellException, TekniskException {
        List<String> virksomhetIDer = List.of(orgnr2);
        forberedValidering();

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(virksomhetIDer, 1L);
        verify(avklartefaktaService, times(1)).leggTilAvklarteFakta(1L, VIRKSOMHET, VIRKSOMHET.getKode(), orgnr2, Avklartefakta.VALGT_FAKTA);
    }

    @Test
    public void lagreVirksomheterSomAvklartefakta_virksomhetErArbeidNorge_valideringOKOgVirksomhetLagret() throws FunksjonellException, TekniskException {
        List<String> virksomhetIDer = List.of(orgnr3);
        forberedValidering();

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(virksomhetIDer, 1L);
        verify(avklartefaktaService, times(1)).leggTilAvklarteFakta(1L, VIRKSOMHET, VIRKSOMHET.getKode(), orgnr3, Avklartefakta.VALGT_FAKTA);
    }

    @Test
    public void lagreVirksomheterSomAvklartefakta_virksomhetErUgyldig_valideringFailerOgVirksomhetIkkeLagret() throws FunksjonellException, TekniskException {
        List<String> virksomhetIDer = List.of(orgnr4);
        forberedValidering();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(virksomhetIDer, 1L))
            .withMessage(String.format("VirksomhetID %s hører ikke til noen av arbeidsforholdene", orgnr4));
        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatyper.class), anyString(), anyString(), eq(Avklartefakta.VALGT_FAKTA));
    }

    @Test
    public void utfyllManglendeAdressefelter_gyldigForretningsadresse_girForretningsadresse() {
        StrukturertAdresse adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(lagOrganisasjonDokument("2345", "Forretningsgatenavn"));

        assertThat(adresse.gatenavn).isEqualTo("Forretningsgatenavn");
        assertThat(adresse.postnummer).isEqualTo("2345");
        assertThat(adresse.poststed).isEqualTo("Poststed");
        assertThat(adresse.landkode).isEqualTo("NO");

        verify(mockKodeverkService).dekod(eq(FellesKodeverk.POSTNUMMER), eq("2345"), any(LocalDate.class));
    }

    @Test
    public void utfyllManglendeAdressefelter_forretningsadresseManglerGatenavn_girForretningsadresseMedBlanktGatenavn() {
        StrukturertAdresse adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(lagOrganisasjonDokument("2345", null));

        assertThat(adresse.gatenavn).isEqualTo(" ");
        assertThat(adresse.postnummer).isEqualTo("2345");
        assertThat(adresse.poststed).isEqualTo("Poststed");
        assertThat(adresse.landkode).isEqualTo("NO");

        verify(mockKodeverkService).dekod(eq(FellesKodeverk.POSTNUMMER), eq("2345"), any(LocalDate.class));
    }

    @Test
    public void utfyllManglendeAdressefelter_utenlandskIngenForretningsadressePostadresseUtenPostnummer_postnummerTomString() {
        var organisasjonDokument = lagOrganisasjonDokument(null, null, null, "DK");
        organisasjonDokument.organisasjonDetaljer.forretningsadresse = Collections.emptyList();
        organisasjonDokument.organisasjonDetaljer.postadresse.stream().findFirst().ifPresent(a -> ((SemistrukturertAdresse)a).setPostnr(null));
        StrukturertAdresse adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(organisasjonDokument);

        assertThat(adresse.gatenavn).isEqualTo("Postgatenavn");
        assertThat(adresse.postnummer).isEqualTo(" ");
        assertThat(adresse.poststed).isEqualTo("Postpoststed");
        assertThat(adresse.landkode).isEqualTo("DK");

        verify(mockKodeverkService, never()).dekod(any(), any(), any());
    }

    @Test
    public void utfyllManglendeAdressefelter_forretningsadresseManglerPostnr_girPostadresse() {
        StrukturertAdresse adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(lagOrganisasjonDokument(null, null));

        assertThat(adresse.gatenavn).isEqualTo("Postgatenavn");
        assertThat(adresse.postnummer).isEqualTo("6789");
        assertThat(adresse.poststed).isEqualTo("Poststed");
        assertThat(adresse.landkode).isEqualTo("NO");

        verify(mockKodeverkService).dekod(eq(FellesKodeverk.POSTNUMMER), eq("6789"), any(LocalDate.class));
    }

    private void forberedValidering() throws FunksjonellException {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.uuid = uuid1;
        SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
        selvstendigForetak.orgnr = orgnr1;
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.arbeidsgiverID = orgnr3;
        ArbeidsforholdDokument arbeidsforholdDokument = new ArbeidsforholdDokument();
        arbeidsforholdDokument.arbeidsforhold.add(arbeidsforhold);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ARBFORH);
        saksopplysning.setDokument(arbeidsforholdDokument);
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.foretakUtland.add(foretakUtland);
        behandlingsgrunnlagData.selvstendigArbeid.selvstendigForetak.add(selvstendigForetak);
        behandlingsgrunnlagData.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add(orgnr2);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        Behandling behandling = new Behandling();
        behandling.setSaksopplysninger(Set.of(saksopplysning));
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
    }

    private void leggTilIRegisterOppslag(Collection<String> orgnumre) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        when(registerOppslagService.hentOrganisasjoner(eq(new HashSet<>(orgnumre)))).thenReturn(lagOrganisasjonDokumenter(orgnumre));
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