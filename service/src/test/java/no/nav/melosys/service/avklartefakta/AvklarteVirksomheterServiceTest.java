package no.nav.melosys.service.avklartefakta;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.adresse.Adresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.OrganisasjonDokumentTestFactory;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VIRKSOMHET;
import static no.nav.melosys.service.MottatteOpplysningerStub.lagMottatteOpplysninger;
import static no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysninger;
import static no.nav.melosys.service.SaksopplysningStubs.lagOrganisasjonDokumenter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvklarteVirksomheterServiceTest {

    @Mock
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private OrganisasjonOppslagService organisasjonOppslagService;

    @Mock
    private KodeverkService mockKodeverkService;

    @Mock
    private BehandlingService behandlingService;

    private Behandling behandling;

    private AvklarteVirksomheterService avklarteVirksomheterService;

    private final String orgnr1 = "111111111";
    private final String orgnr2 = "222222222";
    private final String orgnr3 = "333333333";
    private final String orgnr4 = "444444444";
    private final String uuid1 = "a2k2jf-a3khs";
    private final String uuid2 = "0dkf93-kj701";

    Function<OrganisasjonDokument, Adresse> INGEN_ADRESSE = org -> null;

    @BeforeEach
    public void setUp() {
        behandling = new Behandling();
        behandling.setId(1L);
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(new HashSet<>(Arrays.asList(orgnr1, uuid1)));

        when(mockKodeverkService.dekod(any(FellesKodeverk.class), anyString())).thenReturn("Poststed");

        avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, organisasjonOppslagService, behandlingService, mockKodeverkService);
    }

    @Test
    void hentAntallAvklarteVirksomheter_summererArbeidsgivereOgSelvstendigNæringsdrivendeINorgeOgUtenlandskeVirksomheter() {
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(new HashSet<>(Arrays.asList(orgnr1, orgnr2, orgnr3, orgnr4, uuid1)));
        ForetakUtland foretakUtland1 = lagForetakUtland("Utland1", uuid1, null);
        ForetakUtland foretakUtland2 = lagForetakUtland("Utland2", uuid1, "SE-123456789");
        List<ForetakUtland> foretakUtlandListe = Arrays.asList(foretakUtland1, foretakUtland2);
        List<String> selvstendigeForetak = Arrays.asList(orgnr1, orgnr2);
        List<String> arbeidgivendeEkstraOrgnumre = Arrays.asList(orgnr3, orgnr4);
        Set<Saksopplysning> saksopplysninger = lagArbeidsforholdOpplysninger(Collections.emptyList());
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger(selvstendigeForetak, foretakUtlandListe, arbeidgivendeEkstraOrgnumre));

        int antallAvklarteForetak = avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling);

        assertThat(antallAvklarteForetak).isEqualTo(6);
    }

    @Test
    void hentUtenlandskeVirksomheter_girListeMedKunAvklarteForetak() {
        ForetakUtland foretak1 = lagForetakUtland("Utland1", uuid1, null);
        ForetakUtland foretak2 = lagForetakUtland("Utland2", uuid2, "SE-123456789");
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger(Collections.emptyList(), Arrays.asList(foretak1, foretak2), Collections.emptyList()));

        List<AvklartVirksomhet> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling);
        assertThat(avklarteSelvstendigeOrgnumre.stream().map(av -> av.navn)).containsOnly("Utland1");
    }

    @Test
    void hentUtenlandskeVirksomheter_girListeAvklartVirksomhetMedOrgnrIkkeUuid() {
        ForetakUtland foretak1 = lagForetakUtland("Utland1", uuid1, "SE-123456789");
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger(Collections.emptyList(), Collections.singletonList(foretak1), Collections.emptyList()));

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
    void hentSelvstendigeForetakOrgnumre_girListeMedKunAvklarteOrgnumre() {
        List<String> selvstendigeForetak = Arrays.asList(orgnr1, orgnr2);
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger(selvstendigeForetak, Collections.emptyList(), Collections.emptyList()));

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeSelvstendigeForetakOrgnumre(behandling);
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(orgnr1);
    }

    @Test
    void hentArbeidsgivendeEkstraOrgnumre_girListeMedKunAvklarteOrgnumre() {
        List<String> arbeidgivendeEkstraOrgnumre = Arrays.asList(orgnr2, orgnr1);
        Set<Saksopplysning> saksopplysninger =
            lagArbeidsforholdOpplysninger(Collections.emptyList());
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger(Collections.emptyList(), Collections.emptyList(), arbeidgivendeEkstraOrgnumre));

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling);
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(orgnr1);
    }

    @Test
    void hentArbeidsgivendeRegistreOrgnumre_girListeMedKunAvklarteOrgnumre() {
        List<String> arbeidgivendeOrgnumreEkstra = Arrays.asList(orgnr1, orgnr2, orgnr3);
        Set<Saksopplysning> saksopplysninger =
            lagArbeidsforholdOpplysninger(arbeidgivendeOrgnumreEkstra);
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling);
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(orgnr1);
    }

    @Test
    void testHentAvklarteNorskeForetak_girAvklarteArbeidsgivere() {
        List<String> arbeidsgivereEkstra = Collections.singletonList(orgnr2);
        List<String> arbeidsgivereRegister = Collections.singletonList(orgnr3);

        Set<Saksopplysning> saksopplysninger =
            lagArbeidsforholdOpplysninger(arbeidsgivereRegister);

        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger(Collections.emptyList(), Collections.emptyList(), arbeidsgivereEkstra));

        Set<String> avklarteOrganisasjoner = new HashSet<>(Arrays.asList(orgnr2, orgnr3));
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(avklarteOrganisasjoner);

        leggTilIRegisterOppslag(Arrays.asList(orgnr2, orgnr3));

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService,
            organisasjonOppslagService, behandlingService, mockKodeverkService);
        assertThat(avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, INGEN_ADRESSE).stream()
            .map(nv -> nv.orgnr)
            .collect(Collectors.toList())).contains(orgnr2, orgnr3);
    }

    @Test
    void hentAlleNorskeVirksomheter_SammeOrgNummerFraNorskeArbeidsgivereOgNorskeSelvstendigeForetak_UnngåDuplikater() {
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        ((Logger) LoggerFactory.getLogger("no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService"))
            .addAppender(listAppender);
        listAppender.start();

        List<String> arbeidsgivereRegister = Collections.singletonList(orgnr3);

        Set<Saksopplysning> saksopplysninger =
            lagArbeidsforholdOpplysninger(arbeidsgivereRegister);

        behandling.setSaksopplysninger(saksopplysninger);
        MottatteOpplysninger mottatteOpplysninger = lagMottatteOpplysninger(arbeidsgivereRegister, Collections.emptyList(), Collections.emptyList());
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        Set<String> avklarteOrganisasjoner = new HashSet<>(arbeidsgivereRegister);
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(avklarteOrganisasjoner);

        leggTilIRegisterOppslag(arbeidsgivereRegister);

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService,
            organisasjonOppslagService, behandlingService, mockKodeverkService);
        assertThat(avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, INGEN_ADRESSE))
            .singleElement()
            .matches(avklartVirksomhet -> avklartVirksomhet.getOrgnr().equals(orgnr3));

        assertThat(listAppender.list)
            .singleElement()
            .extracting(ILoggingEvent::getMessage)
            .isEqualTo("Fant selvstendige foretak med samme orgnummer({}) som allerede er hentet fra norskeArbeidsgivere");
    }

    @Test
    void hentAvklarteNorskeForetak_girAvklarteSelvstendigeForetak() {
        List<String> selvstendigeForetak = Collections.singletonList(orgnr1);

        Set<Saksopplysning> saksopplysninger =
            lagArbeidsforholdOpplysninger(Collections.emptyList());

        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger(selvstendigeForetak, Collections.emptyList(), Collections.emptyList()));

        Set<String> avklarteOrganisasjoner = new HashSet<>(selvstendigeForetak);
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(avklarteOrganisasjoner);

        leggTilIRegisterOppslag(selvstendigeForetak);

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService,
            organisasjonOppslagService, behandlingService, mockKodeverkService);
        assertThat(avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, INGEN_ADRESSE).stream()
            .map(nv -> nv.orgnr)
            .collect(Collectors.toList())).contains(orgnr1);
    }

    @Test
    void harOpphørtAvklartVirksomhet_opphoersdatoTilbakeITid_girTrue() {
        OrganisasjonDokument orgDok = lagOrganisasjonDokument("0011", "Gatenavn 1");
        orgDok.getOrganisasjonDetaljer().setOpphoersdato(LocalDate.now().minusYears(1));
        when(organisasjonOppslagService.hentOrganisasjoner(any())).thenReturn(Collections.singleton(orgDok));

        behandling.setSaksopplysninger(lagArbeidsforholdOpplysninger(Collections.emptyList()));
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));


        boolean harOpphørtAvklartVirksomhet = avklarteVirksomheterService.harOpphørtAvklartVirksomhet(behandling);


        assertThat(harOpphørtAvklartVirksomhet).isTrue();
    }

    @Test
    void lagreVirksomheterSomAvklartefakta_virksomhetErForetakUtland_valideringOKOgVirksomhetLagret() {
        List<String> virksomhetIDer = List.of(uuid1);
        forberedValidering();

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer);
        verify(avklartefaktaService, times(1)).leggTilAvklarteFakta(1L, VIRKSOMHET, VIRKSOMHET.getKode(), uuid1, Avklartefakta.VALGT_FAKTA);
    }

    @Test
    void lagreVirksomheterSomAvklartefakta_virksomhetErSelvstendigForetak_valideringOKOgVirksomhetLagret() {
        List<String> virksomhetIDer = List.of(orgnr1);
        forberedValidering();

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer);
        verify(avklartefaktaService, times(1)).leggTilAvklarteFakta(1L, VIRKSOMHET, VIRKSOMHET.getKode(), orgnr1, Avklartefakta.VALGT_FAKTA);
    }

    @Test
    void lagreVirksomheterSomAvklartefakta_virksomhetErLagtInnManuelt_valideringOKOgVirksomhetLagret() {
        List<String> virksomhetIDer = List.of(orgnr2);
        forberedValidering();

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer);
        verify(avklartefaktaService, times(1)).leggTilAvklarteFakta(1L, VIRKSOMHET, VIRKSOMHET.getKode(), orgnr2, Avklartefakta.VALGT_FAKTA);
    }

    @Test
    void lagreVirksomheterSomAvklartefakta_virksomhetErArbeidNorge_valideringOKOgVirksomhetLagret() {
        List<String> virksomhetIDer = List.of(orgnr3);
        forberedValidering();

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer);
        verify(avklartefaktaService, times(1)).leggTilAvklarteFakta(1L, VIRKSOMHET, VIRKSOMHET.getKode(), orgnr3, Avklartefakta.VALGT_FAKTA);
    }

    @Test
    void lagreVirksomheterSomAvklartefakta_virksomhetErUgyldig_valideringFailerOgVirksomhetIkkeLagret() {
        List<String> virksomhetIDer = List.of(orgnr4);
        forberedValidering();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer))
            .withMessage(String.format("VirksomhetID %s hører ikke til noen av arbeidsforholdene", orgnr4));
        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatyper.class), anyString(), anyString(), eq(Avklartefakta.VALGT_FAKTA));
    }

    @Test
    void utfyllManglendeAdressefelter_gyldigForretningsadresse_girForretningsadresse() {
        StrukturertAdresse adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(lagOrganisasjonDokument("2345", "Forretningsgatenavn"));

        assertThat(adresse.getGatenavn()).isEqualTo("Forretningsgatenavn");
        assertThat(adresse.getPostnummer()).isEqualTo("2345");
        assertThat(adresse.getPoststed()).isEqualTo("Poststed");
        assertThat(adresse.getLandkode()).isEqualTo("NO");

        verify(mockKodeverkService).dekod(FellesKodeverk.POSTNUMMER, "2345");
    }

    @Test
    void utfyllManglendeAdressefelter_forretningsadresseManglerGatenavn_girForretningsadresseMedBlanktGatenavn() {
        StrukturertAdresse adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(lagOrganisasjonDokument("2345", null));

        assertThat(adresse.getGatenavn()).isEqualTo(" ");
        assertThat(adresse.getPostnummer()).isEqualTo("2345");
        assertThat(adresse.getPoststed()).isEqualTo("Poststed");
        assertThat(adresse.getLandkode()).isEqualTo("NO");

        verify(mockKodeverkService).dekod(FellesKodeverk.POSTNUMMER, "2345");
    }

    @Test
    void utfyllManglendeAdressefelter_utenlandskIngenForretningsadressePostadresseUtenPostnummer_postnummerTomString() {
        var organisasjonDokument = lagOrganisasjonDokument(null, null, null, "DK");
        organisasjonDokument.getOrganisasjonDetaljer().setForretningsadresse(Collections.emptyList());
        organisasjonDokument.getOrganisasjonDetaljer().getPostadresse().stream().findFirst().ifPresent(a -> ((SemistrukturertAdresse) a).setPostnr(null));
        StrukturertAdresse adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(organisasjonDokument);

        assertThat(adresse.getGatenavn()).isEqualTo("Postgatenavn");
        assertThat(adresse.getPostnummer()).isEqualTo(" ");
        assertThat(adresse.getPoststed()).isEqualTo("Postpoststed");
        assertThat(adresse.getLandkode()).isEqualTo("DK");

        verify(mockKodeverkService, never()).dekod(any(), any());
    }

    @Test
    void utfyllManglendeAdressefelter_forretningsadresseManglerPostnr_girPostadresse() {
        StrukturertAdresse adresse = avklarteVirksomheterService.utfyllManglendeAdressefelter(lagOrganisasjonDokument(null, null));

        assertThat(adresse.getGatenavn()).isEqualTo("Postgatenavn");
        assertThat(adresse.getPostnummer()).isEqualTo("6789");
        assertThat(adresse.getPoststed()).isEqualTo("Poststed");
        assertThat(adresse.getLandkode()).isEqualTo("NO");

        verify(mockKodeverkService).dekod(FellesKodeverk.POSTNUMMER, "6789");
    }

    private void forberedValidering() {
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
        MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.foretakUtland.add(foretakUtland);
        mottatteOpplysningerData.selvstendigArbeid.selvstendigForetak.add(selvstendigForetak);
        mottatteOpplysningerData.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add(orgnr2);
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerdata(mottatteOpplysningerData);
        Behandling behandling = new Behandling();
        behandling.setSaksopplysninger(Set.of(saksopplysning));
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
    }

    private void leggTilIRegisterOppslag(Collection<String> orgnumre) {
        when(organisasjonOppslagService.hentOrganisasjoner(new HashSet<>(orgnumre))).thenReturn(lagOrganisasjonDokumenter(orgnumre));
    }

    private OrganisasjonDokument lagOrganisasjonDokument(String forretningsPostnr, String forretningsGatenavn) {
        return lagOrganisasjonDokument(forretningsPostnr, forretningsGatenavn, "6789", "NO");
    }

    private OrganisasjonDokument lagOrganisasjonDokument(String forretningsPostnr, String forretningsGatenavn, String postadressePostnr, String postadresseLand) {
        OrganisasjonDokument organisasjonDokument = OrganisasjonDokumentTestFactory.createOrganisasjonDokumentForTest();
        OrganisasjonsDetaljer organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonDokument.setOrganisasjonDetaljer(organisasjonsDetaljer);
        SemistrukturertAdresse forretningsadresse = new SemistrukturertAdresse();
        organisasjonsDetaljer.setForretningsadresse(List.of(forretningsadresse));
        forretningsadresse.setAdresselinje1(forretningsGatenavn);
        forretningsadresse.setPostnr(forretningsPostnr);
        forretningsadresse.setPoststed("Forretningspoststed");
        forretningsadresse.setLandkode("NO");
        forretningsadresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
        SemistrukturertAdresse postadresse = new SemistrukturertAdresse();
        organisasjonsDetaljer.setPostadresse(List.of(postadresse));
        postadresse.setAdresselinje1("Postgatenavn");
        postadresse.setPostnr(postadressePostnr);
        postadresse.setPoststed("Postpoststed");
        postadresse.setLandkode(postadresseLand);
        postadresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

        return organisasjonDokument;
    }
}
