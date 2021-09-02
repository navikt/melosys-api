package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.*;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokgenMalMapperTest {
    public static final String ADRESSELINJE_1_BRUKER = "Andebygata 1";
    public static final String FORRETNINGSADRESSE_ORG = "Storgata 1";
    public static final String KONTAKT_NAVN = "Fetter Anton";
    public static final String NAVN_ORG = "Advokatene AS";
    public static final String POSTBOKS_ORG = "POSTBOKS 200";
    public static final String POSTNR_BRUKER = "9999";
    public static final String POSTNR_ORG = "9990";
    public static final String SAMMENSATT_NAVN = "Donald Duck";

    @Mock
    private BehandlingsresultatService mockBehandlingsresultatService;
    @Mock
    private EregFasade mockEregFasade;
    @Mock
    private KodeverkService mockKodeverkService;
    @Mock
    private PersondataFasade mockPersondataFasade;

    @Mock
    private InnvilgelseFtrlMapper mockInnvilgelseFtrlMapper;

    private final FakeUnleash fakeUnleash = new FakeUnleash();

    private DokgenMalMapper dokgenMalMapper;

    @BeforeEach
    void init() {
        dokgenMalMapper = new DokgenMalMapper(mockBehandlingsresultatService,
            mockEregFasade, mockKodeverkService, mockPersondataFasade, fakeUnleash, mockInnvilgelseFtrlMapper);
    }

    @Test
    void feilerNårProduserbartDokumentIkkeErStøttet() {
        when(mockPersondataFasade.hentPersonFraTps(any(), any())).thenReturn(lagPersonopplysning());

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(ATTEST_A1)
            .medBehandling(lagBehandling(lagFagsak()))
            .build();

        assertThrows(FunksjonellException.class, () ->
            dokgenMalMapper.mapBehandling(brevbestilling)
        );
    }

    @Test
    void skalMappeMedBrukerAdresse() {
        when(mockKodeverkService.dekod(any(), any())).thenReturn("Andeby");
        when(mockPersondataFasade.hentPersonFraTps(any(), any())).thenReturn(lagPersonopplysning());

        Behandling behandling = lagBehandling(lagFagsak());

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);

        assertTrue(dokgenDto instanceof SaksbehandlingstidSoknad);
        assertEquals(SAMMENSATT_NAVN, dokgenDto.getNavnBruker());
        assertEquals(SAMMENSATT_NAVN, dokgenDto.getNavnMottaker());
        assertEquals(ADRESSELINJE_1_BRUKER, dokgenDto.getAdresselinjer().get(0));
        assertEquals(POSTNR_BRUKER, dokgenDto.getPostnr());
    }

    @Test
    void mapping_persondataFraPdl_ok() {
        fakeUnleash.enable("melosys.brev.adresser.pdl");
        when(mockKodeverkService.dekod(any(), any())).thenReturn("Andeby");
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn((Persondata) lagPersonopplysning().getDokument());

        Behandling behandling = lagBehandling(lagFagsak());

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);

        assertTrue(dokgenDto instanceof SaksbehandlingstidSoknad);
        assertEquals(SAMMENSATT_NAVN, dokgenDto.getNavnBruker());
        assertEquals(SAMMENSATT_NAVN, dokgenDto.getNavnMottaker());
        assertEquals(ADRESSELINJE_1_BRUKER, dokgenDto.getAdresselinjer().get(0));
        assertEquals(POSTNR_BRUKER, dokgenDto.getPostnr());
        fakeUnleash.disableAll();
    }

    @Test
    void skalMappeMedFullmektigAdresse() {
        when(mockKodeverkService.dekod(any(), any())).thenReturn("Andeby");
        when(mockPersondataFasade.hentPersonFraTps(any(), any())).thenReturn(lagPersonopplysning());

        Behandling behandling = lagBehandling(lagFagsak());

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);

        assertTrue(dokgenDto instanceof SaksbehandlingstidSoknad);
        assertEquals(SAMMENSATT_NAVN, dokgenDto.getNavnBruker());
        assertEquals(NAVN_ORG, dokgenDto.getNavnMottaker());
        assertEquals(POSTBOKS_ORG, dokgenDto.getAdresselinjer().get(0));
        assertEquals(POSTNR_ORG, dokgenDto.getPostnr());
    }

    @Test
    void skalMappeMedFullmektigForretningsAdresse() {
        when(mockKodeverkService.dekod(any(), any())).thenReturn("Andeby");
        when(mockPersondataFasade.hentPersonFraTps(any(), any())).thenReturn(lagPersonopplysning());

        Behandling behandling = lagBehandling(lagFagsak());

        OrganisasjonDokument org = lagOrg();
        org.getOrganisasjonDetaljer().forretningsadresse = singletonList(lagOrgForretningsadresse());
        org.getOrganisasjonDetaljer().postadresse = emptyList();

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .medBehandling(behandling)
            .medOrg(org)
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);

        assertTrue(dokgenDto instanceof SaksbehandlingstidSoknad);
        assertEquals(SAMMENSATT_NAVN, dokgenDto.getNavnBruker());
        assertEquals(NAVN_ORG, dokgenDto.getNavnMottaker());
        assertEquals(FORRETNINGSADRESSE_ORG, dokgenDto.getAdresselinjer().get(0));
        assertEquals(POSTNR_ORG, dokgenDto.getPostnr());
    }

    @Test
    void skalMappeMedFullmektigMedKontaktpersonAdresse() {
        when(mockKodeverkService.dekod(any(), any())).thenReturn("Andeby");
        when(mockPersondataFasade.hentPersonFraTps(any(), any())).thenReturn(lagPersonopplysning());

        Behandling behandling = lagBehandling(lagFagsak());

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);

        assertTrue(dokgenDto instanceof SaksbehandlingstidSoknad);
        assertEquals(SAMMENSATT_NAVN, dokgenDto.getNavnBruker());
        assertEquals(NAVN_ORG, dokgenDto.getNavnMottaker());
        assertEquals("Att: " + KONTAKT_NAVN, dokgenDto.getAdresselinjer().get(0));
        assertEquals(POSTBOKS_ORG, dokgenDto.getAdresselinjer().get(1));
        assertEquals(POSTNR_ORG, dokgenDto.getPostnr());
    }

    @Test
    void skalMappeMangelbrevTilBruker() {
        when(mockKodeverkService.dekod(any(), any())).thenReturn("Andeby");
        when(mockPersondataFasade.hentPersonFraTps(any(), any())).thenReturn(lagPersonopplysning());

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .medManglerInfoFritekst("Dummy")
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);
        assertTrue(dokgenDto instanceof MangelbrevBruker);
        MangelbrevBruker result = (MangelbrevBruker) dokgenDto;
        assertEquals("Dummy", result.getInnledningFritekst());
        assertEquals("Dummy", result.getManglerInfoFritekst());
        assertEquals(Instant.now().plus(Period.ofWeeks(4)).truncatedTo(ChronoUnit.DAYS), result.getDatoInnsendingsfrist().truncatedTo(ChronoUnit.DAYS));
    }

    @Test
    void skalMappeMangelbrevTilArbeidsgiver() {
        when(mockKodeverkService.dekod(any(), any())).thenReturn("Andeby");
        when(mockPersondataFasade.hentPersonFraTps(any(), any())).thenReturn(lagPersonopplysning());
        when(mockEregFasade.hentOrganisasjonNavn(any())).thenReturn("Fullmektig AS");

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_ARBEIDSGIVER)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .medManglerInfoFritekst("Dummy")
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);
        assertTrue(dokgenDto instanceof MangelbrevArbeidsgiver);
        MangelbrevArbeidsgiver result = (MangelbrevArbeidsgiver) dokgenDto;
        assertEquals("Dummy", result.getInnledningFritekst());
        assertEquals("Dummy", result.getManglerInfoFritekst());
        assertEquals("Fullmektig AS", result.getNavnFullmektig());
        assertEquals(Instant.now().plus(Period.ofWeeks(4)).truncatedTo(ChronoUnit.DAYS), result.getDatoInnsendingsfrist().truncatedTo(ChronoUnit.DAYS));
    }

    @Test
    @Disabled
    void skalMappeInnvilgelsesbrevTilBruker() {
        when(mockKodeverkService.dekod(any(), any())).thenReturn("Andeby");
        when(mockPersondataFasade.hentPersonFraTps(any(), any())).thenReturn(lagPersonopplysning());
        when(mockInnvilgelseFtrlMapper.map(any())).thenReturn(lagInnvilgelseFtrl());

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new InnvilgelseBrevbestilling.Builder()
            .medProduserbartdokument(INNVILGELSE_FOLKETRYGDLOVEN_2_8)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);
        assertTrue(dokgenDto instanceof MangelbrevBruker);
        MangelbrevBruker result = (MangelbrevBruker) dokgenDto;
        assertEquals("Dummy", result.getInnledningFritekst());
        assertEquals("Dummy", result.getManglerInfoFritekst());
        assertEquals(Instant.now().plus(Period.ofWeeks(4)).truncatedTo(ChronoUnit.DAYS), result.getDatoInnsendingsfrist().truncatedTo(ChronoUnit.DAYS));
    }

    private InnvilgelseFtrl lagInnvilgelseFtrl() {
        return new InnvilgelseFtrl(
            new InnvilgelseBrevbestilling(),
            null,
            true,
            Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_MULTINASJONALT_SELSKAP.getBeskrivelse(),
            false,
            false,
            null,
            null,
            null,
            "Egon Olsen AS",
            "USA",
            false,
            null,
            null,
            null,
            false,
            String.valueOf(LocalDate.now().getYear()),
            false,
            false
        );
    }

    private Fagsak lagFagsak() {
        return lagFagsak(false);
    }

    private Fagsak lagFagsak(boolean medRepresentant) {
        Fagsak fagsak = new Fagsak();
        fagsak.setRegistrertDato(Instant.now());
        fagsak.setBehandlinger(lagBehandlinger());
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setEndretAv("L12345");
        Aktoer bruker = new Aktoer();
        bruker.setRolle(BRUKER);
        bruker.setAktørId("aktørId");
        fagsak.getAktører().add(bruker);
        if (medRepresentant) {
            Aktoer representant = new Aktoer();
            representant.setRolle(Aktoersroller.REPRESENTANT);
            representant.setRepresenterer(Representerer.BRUKER);
            fagsak.getAktører().add(representant);
        }
        return fagsak;
    }

    private Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(singleton(lagPersonopplysning()));
        behandling.setFagsak(fagsak);
        return behandling;
    }

    private List<Behandling> lagBehandlinger() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        return singletonList(behandling);
    }

    private Saksopplysning lagPersonopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        PersonDokument personDokument = new PersonDokument();
        personDokument.setFnr("99887766554");
        personDokument.setSammensattNavn(SAMMENSATT_NAVN);
        personDokument.setGjeldendePostadresse(lagAdresse());
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }

    private UstrukturertAdresse lagAdresse() {
        UstrukturertAdresse ustrukturertAdresse = new UstrukturertAdresse();
        ustrukturertAdresse.adresselinje1 = ADRESSELINJE_1_BRUKER;
        ustrukturertAdresse.postnr = POSTNR_BRUKER;
        return ustrukturertAdresse;
    }

    private OrganisasjonDokument lagOrg() {
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setNavn(NAVN_ORG);
        organisasjonDokument.setOrganisasjonDetaljer(lagOrgDetaljer());
        return organisasjonDokument;
    }

    private OrganisasjonsDetaljer lagOrgDetaljer() {
        OrganisasjonsDetaljer organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonsDetaljer.postadresse = singletonList(lagOrgAdresse());
        return organisasjonsDetaljer;
    }

    private GeografiskAdresse lagOrgAdresse() {
        SemistrukturertAdresse semistrukturertAdresse = new SemistrukturertAdresse();
        semistrukturertAdresse.setAdresselinje1(POSTBOKS_ORG);
        semistrukturertAdresse.setPostnr(POSTNR_ORG);
        semistrukturertAdresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2)));
        return semistrukturertAdresse;
    }

    private GeografiskAdresse lagOrgForretningsadresse() {
        SemistrukturertAdresse semistrukturertAdresse = new SemistrukturertAdresse();
        semistrukturertAdresse.setAdresselinje1(FORRETNINGSADRESSE_ORG);
        semistrukturertAdresse.setPostnr(POSTNR_ORG);
        semistrukturertAdresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2)));
        return semistrukturertAdresse;
    }

    private Kontaktopplysning lagKontaktOpplysning() {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktNavn(KONTAKT_NAVN);
        return kontaktopplysning;
    }
}
