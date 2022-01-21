package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Innvilgelse;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Person;
import no.nav.melosys.integrasjon.dokgen.dto.storbritannia.attest.*;
import no.nav.melosys.integrasjon.dokgen.dto.storbritannia.innvilgelse.InnvilgelseStorbritannia;
import no.nav.melosys.integrasjon.dokgen.dto.storbritannia.innvilgelse.Soknad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.TRYGDEMYNDIGHET;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.dokument.DokgenTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokgenMalMapperTest {
    public static final String FORRETNINGSADRESSE_ORG = "Storgata 1";
    public static final LocalDate SOKNADSDATO = LocalDate.of(2000, 1, 1);
    public static final LocalDate LOVVALGSPERIODE_FOM = LocalDate.of(2020, 1, 1);
    public static final LocalDate LOVVALGSPERIODE_TOM = LocalDate.of(2021, 1, 1);
    public static final LocalDate FØDSELSDATO = LocalDate.of(2000, 1, 1);

    @Mock
    private InnvilgelseFtrlMapper mockInnvilgelseFtrlMapper;
    @Mock
    private DokgenMapperDatahenter mockDokgenMapperDatahenter;
    @Mock
    private StorbritanniaMapper mockStorbritanniaMapper;

    private final FakeUnleash fakeUnleash = new FakeUnleash();

    private DokgenMalMapper dokgenMalMapper;

    @BeforeEach
    void init() {
        dokgenMalMapper = new DokgenMalMapper(mockDokgenMapperDatahenter, mockInnvilgelseFtrlMapper, mockStorbritanniaMapper);
    }

    @Test
    void feilerNårProduserbartDokumentIkkeErStøttet() {
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(ATTEST_A1)
            .medBehandling(lagBehandling())
            .build();

        assertThatThrownBy(() -> dokgenMalMapper.mapBehandling(brevbestilling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("ProduserbartDokument ATTEST_A1 er ikke støttet av melosys-dokgen");
    }

    @Test
    void skalMappeMedBrukerAdresse() {
        when(mockDokgenMapperDatahenter.hentPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());

        Behandling behandling = lagBehandling();

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);

        assertThat(dokgenDto)
            .isInstanceOf(SaksbehandlingstidSoknad.class)
            .extracting(
                dto -> dto.getSaksopplysninger().navnBruker(),
                dto -> dto.getMottaker().navn(),
                dto -> dto.getMottaker().postnr()
            ).containsExactly(
            SAMMENSATT_NAVN_BRUKER,
            SAMMENSATT_NAVN_BRUKER,
            POSTNR_BRUKER
        );
        assertThat(dokgenDto.getMottaker().adresselinjer()).contains(ADRESSELINJE_1_BRUKER);
    }

    @Test
    void mapping_persondataFraPdl_ok() {
        fakeUnleash.enable("melosys.pdl.aktiv");
        when(mockDokgenMapperDatahenter.hentPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());

        Behandling behandling = lagBehandling();

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);

        assertThat(dokgenDto)
            .isInstanceOf(SaksbehandlingstidSoknad.class)
            .extracting(
                dto -> dto.getSaksopplysninger().navnBruker(),
                dto -> dto.getMottaker().navn(),
                dto -> dto.getMottaker().postnr()
            ).containsExactly(
            SAMMENSATT_NAVN_BRUKER,
            SAMMENSATT_NAVN_BRUKER,
            POSTNR_BRUKER
        );
        assertThat(dokgenDto.getMottaker().adresselinjer()).contains(ADRESSELINJE_1_BRUKER);
        fakeUnleash.disableAll();
    }

    @Test
    void mapping_avsenderMyndighet_ok() {
        when(mockDokgenMapperDatahenter.hentLandnavn("FI")).thenReturn("Finland");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());

        Behandling behandling = lagBehandling();

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .medAvsendertype(Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET)
            .medAvsenderLand("FI")
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);

        assertThat(dokgenDto)
            .isInstanceOf(SaksbehandlingstidSoknad.class);

        assertThat((SaksbehandlingstidSoknad) dokgenDto)
            .extracting(
                SaksbehandlingstidSoknad::getAvsenderTypeSoknad,
                SaksbehandlingstidSoknad::getAvsenderLand
            ).containsExactly(
            TRYGDEMYNDIGHET,
            "Finland"
        );
    }

    @Test
    void skalMappeMedFullmektigAdresse() {
        when(mockDokgenMapperDatahenter.hentPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());

        Behandling behandling = lagBehandling();

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);

        assertThat(dokgenDto)
            .isInstanceOf(SaksbehandlingstidSoknad.class)
            .extracting(
                dto -> dto.getSaksopplysninger().navnBruker(),
                dto -> dto.getMottaker().navn(),
                dto -> dto.getMottaker().postnr()
            ).containsExactly(
            SAMMENSATT_NAVN_BRUKER,
            NAVN_ORG,
            POSTNR_ORG
        );
        assertThat(dokgenDto.getMottaker().adresselinjer()).contains(POSTBOKS_ORG);
    }

    @Test
    void skalMappeMedFullmektigForretningsAdresse() {
        when(mockDokgenMapperDatahenter.hentPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());

        Behandling behandling = lagBehandling();

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

        assertThat(dokgenDto)
            .isInstanceOf(SaksbehandlingstidSoknad.class)
            .extracting(
                dto -> dto.getSaksopplysninger().navnBruker(),
                dto -> dto.getMottaker().navn(),
                dto -> dto.getMottaker().postnr()
            ).containsExactly(
            SAMMENSATT_NAVN_BRUKER,
            NAVN_ORG,
            POSTNR_ORG
        );
        assertThat(dokgenDto.getMottaker().adresselinjer()).contains(FORRETNINGSADRESSE_ORG);
    }

    @Test
    void skalMappeMedFullmektigMedKontaktpersonAdresse() {
        when(mockDokgenMapperDatahenter.hentPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());

        Behandling behandling = lagBehandling();

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);

        assertThat(dokgenDto)
            .isInstanceOf(SaksbehandlingstidSoknad.class)
            .extracting(
                dto -> dto.getSaksopplysninger().navnBruker(),
                dto -> dto.getMottaker().navn(),
                dto -> dto.getMottaker().postnr()
            ).containsExactly(
            SAMMENSATT_NAVN_BRUKER,
            NAVN_ORG,
            POSTNR_ORG
        );
        assertThat(dokgenDto.getMottaker().adresselinjer()).containsExactly("Att: " + KONTAKT_NAVN, POSTBOKS_ORG);
    }

    @Test
    void skalMappeMangelbrevTilBruker() {
        when(mockDokgenMapperDatahenter.hentPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());

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

        assertThat(dokgenDto)
            .isInstanceOf(MangelbrevBruker.class);

        assertThat((MangelbrevBruker) dokgenDto)
            .extracting(
                MangelbrevBruker::getInnledningFritekst,
                MangelbrevBruker::getManglerInfoFritekst
            ).containsExactly(
            "Dummy",
            "Dummy"
        );
        assertThat(((MangelbrevBruker) dokgenDto).getDatoInnsendingsfrist().truncatedTo(ChronoUnit.DAYS))
            .isEqualTo(Instant.now().plus(Period.ofWeeks(4)).truncatedTo(ChronoUnit.DAYS));
    }

    @Test
    void skalMappeMangelbrevTilArbeidsgiver() {
        when(mockDokgenMapperDatahenter.hentPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());
        when(mockDokgenMapperDatahenter.hentFullmektigNavn(any(), eq(Representerer.BRUKER))).thenReturn("Fullmektig AS");

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

        assertThat(dokgenDto)
            .isInstanceOf(MangelbrevArbeidsgiver.class);

        assertThat((MangelbrevArbeidsgiver) dokgenDto)
            .extracting(
                MangelbrevArbeidsgiver::getInnledningFritekst,
                MangelbrevArbeidsgiver::getManglerInfoFritekst,
                MangelbrevArbeidsgiver::getNavnFullmektig
            ).containsExactly(
            "Dummy",
            "Dummy",
            "Fullmektig AS"
        );
        assertThat(((MangelbrevArbeidsgiver) dokgenDto).getDatoInnsendingsfrist().truncatedTo(ChronoUnit.DAYS))
            .isEqualTo(Instant.now().plus(Period.ofWeeks(4)).truncatedTo(ChronoUnit.DAYS));
    }

    @Test
    void skalMappeInnvilgelsesbrevTilBruker() {
        when(mockDokgenMapperDatahenter.hentPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());
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

        assertThat(dokgenMalMapper.mapBehandling(brevbestilling))
            .isInstanceOf(InnvilgelseFtrl.class);
    }

    @Test
    void skalMappeStorbritanniabrev() {
        when(mockDokgenMapperDatahenter.hentPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());
        when(mockStorbritanniaMapper.map(any())).thenReturn(lagInnvilgelseOgAttestStorbritannia());

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new InnvilgelseBrevbestilling.Builder()
            .medProduserbartdokument(STORBRITANNIA)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);
        assertThat(dokgenDto).isInstanceOf(InnvilgelseOgAttestStorbritannia.class);

        InnvilgelseStorbritannia innvilgelse = ((InnvilgelseOgAttestStorbritannia) dokgenDto).getInnvilgelse();
        AttestStorbritannia attest = ((InnvilgelseOgAttestStorbritannia) dokgenDto).getAttest();

        assertThat(innvilgelse).extracting(
            i -> i.getInnvilgelse().innledningFritekst(),
            i -> i.getArtikkel(),
            i -> i.getSoknad().virksomhetsnavn(),
            i -> i.isVirksomhetArbeidsgiverSkalHaKopi()
        ).containsExactly(
            "Innledning",
            Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1,
            "Virksomhetsnavn",
            true
        );

        assertThat(attest)
            .extracting(
                a -> a.getArbeidstaker().navn(),
                a -> a.getArbeidstaker().fnr(),
                a -> a.getArbeidsgiverNorge().fullstendigAdresse(),
                a -> a.getArbeidsgiverNorge().virksomhetsnavn(),
                a -> a.getUtsendelse().artikkel(),
                a -> a.getMedfolgendeFamiliemedlemmer().ektefelle().navn(),
                a -> a.getMedfolgendeFamiliemedlemmer().barn(),
                a -> a.getRepresentant().navn()
            ).containsExactly(
            "Nordmann, Ola",
            "01010119901",
            List.of("Nordmannsveg 200", "Norge"),
            "Virksomhetsnavn",
            Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1,
            "Kone",
            List.of(),
            "Mrs. London"
        );
    }

    @Test
    void skalMappeFritekstbrevTilBruker() {
        when(mockDokgenMapperDatahenter.hentPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());
        when(mockDokgenMapperDatahenter.hentFullmektigNavn(any(), eq(Representerer.BRUKER))).thenReturn("Fullmektig AS");

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medBehandling(behandling)
            .medFritekstTittel("Min tittel")
            .medFritekst("Innhold")
            .medKontaktopplysninger(true)
            .medSaksbehandlerNavn("Fetter Anton")
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);
        assertThat(dokgenDto).isInstanceOf(Fritekstbrev.class);

        assertThat((Fritekstbrev) dokgenDto)
            .extracting(
                Fritekstbrev::getFritekstTittel,
                Fritekstbrev::getFritekst,
                Fritekstbrev::isMedKontaktopplysninger,
                Fritekstbrev::getNavnFullmektig,
                Fritekstbrev::getSaksbehandlerNavn
            ).containsExactly(
            "Min tittel",
            "Innhold",
            true,
            "Fullmektig AS",
            "Fetter Anton"
        );

        assertThat(dokgenDto.getMottaker().type()).isEqualTo(Aktoersroller.BRUKER.getKode());
    }

    @Test
    void skalMappeFritekstbrevTilArbeidsgiver() {
        when(mockDokgenMapperDatahenter.hentPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersonDokument());
        when(mockDokgenMapperDatahenter.hentFullmektigNavn(any(), eq(Representerer.ARBEIDSGIVER))).thenReturn(null);

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_ARBEIDSGIVER)
            .medBehandling(behandling)
            .medFritekstTittel("Min tittel")
            .medFritekst("Innhold")
            .medKontaktopplysninger(true)
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling);
        assertThat(dokgenDto).isInstanceOf(Fritekstbrev.class);

        assertThat((Fritekstbrev) dokgenDto)
            .extracting(
                Fritekstbrev::getFritekstTittel,
                Fritekstbrev::getFritekst,
                Fritekstbrev::isMedKontaktopplysninger,
                Fritekstbrev::getNavnFullmektig
            ).containsExactly(
            "Min tittel",
            "Innhold",
            true,
            null
        );

        assertThat(dokgenDto.getMottaker().type()).isEqualTo(Aktoersroller.ARBEIDSGIVER.getKode());
    }

    private InnvilgelseBrevbestilling lagInnvilgelseBrevbestilling() {
        return new InnvilgelseBrevbestilling.Builder()
            .medInnledningFritekst("Innledning")
            .medBehandling(lagBehandling())
            .medPersonDokument(lagPersonDokument())
            .build();
    }

    private InnvilgelseFtrl lagInnvilgelseFtrl() {
        return new InnvilgelseFtrl.Builder(lagInnvilgelseBrevbestilling())
            .perioder(null)
            .erFullstendigInnvilget(true)
            .ftrl_2_8_begrunnelse(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_MULTINASJONALT_SELSKAP.getBeskrivelse())
            .vurderingMedlemskapEktefelle(false)
            .vurderingLovvalgBarn(false)
            .omfattetFamilie(null)
            .ikkeOmfattetEktefelle(null)
            .ikkeOmfattetBarn(null)
            .arbeidsgiverNavn("Egon Olsen AS")
            .arbeidsland("USA")
            .trygdeavtaleMedArbeidsland(false)
            .vurderingTrygdeavgift(null)
            .loennsforhold(null)
            .arbeidsgiverFullmektigNavn(null)
            .avgiftssatsAar(String.valueOf(LocalDate.now().getYear()))
            .loennNorgeSkattepliktig(false)
            .loennUtlandSkattepliktig(false)
            .build();
    }

    private InnvilgelseOgAttestStorbritannia lagInnvilgelseOgAttestStorbritannia() {
        return new InnvilgelseOgAttestStorbritannia.Builder(lagInnvilgelseBrevbestilling())
            .innvilgelse(lagInnvilgelseStorbritannia())
            .attest(lagAttestStorbritannia())
            .build();
    }

    private InnvilgelseStorbritannia lagInnvilgelseStorbritannia() {
        return new InnvilgelseStorbritannia.Builder()
            .innvilgelse(Innvilgelse.av(lagInnvilgelseBrevbestilling()))
            .artikkel(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1)
            .soknad(new Soknad(SOKNADSDATO,
                LOVVALGSPERIODE_FOM,
                LOVVALGSPERIODE_TOM,
                "Virksomhetsnavn"
            ))
            .familie(null)
            .virksomhetArbeidsgiverSkalHaKopi(true)
            .build();
    }

    private AttestStorbritannia lagAttestStorbritannia() {
        DokgenBrevbestilling dokgenBrevbestillingBuilder = new DokgenBrevbestilling()
            .toBuilder()
            .medBehandling(lagBehandling())
            .medPersonDokument(lagPersonDokument())
            .build();
        return new AttestStorbritannia.Builder(dokgenBrevbestillingBuilder)
            .medfolgendeFamiliemedlemmer(new MedfolgendeFamiliemedlemmer(
                new Person("Kone",
                    FØDSELSDATO,
                    "01010119901",
                    null),
                List.of()
            ))
            .arbeidsgiverNorge(new ArbeidsgiverNorge(
                "Virksomhetsnavn", List.of("Nordmannsveg 200", "Norge")))
            .arbeidstaker(
                new Arbeidstaker(
                    "Nordmann, Ola",
                    LocalDate.now().minusDays(20),
                    "01010119901",
                    List.of("Nordmannsveg 200", "Norge")))
            .representant(new RepresentantStorbritannia(
                "Mrs. London",
                List.of("UK Street 1337"))
            )
            .utsendelse(new Utsendelse(
                Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1,
                List.of("UK Street 1337"),
                LOVVALGSPERIODE_FOM,
                LOVVALGSPERIODE_TOM
            ))
            .build();
    }

    private GeografiskAdresse lagOrgForretningsadresse() {
        SemistrukturertAdresse semistrukturertAdresse = new SemistrukturertAdresse();
        semistrukturertAdresse.setAdresselinje1(FORRETNINGSADRESSE_ORG);
        semistrukturertAdresse.setPostnr(POSTNR_ORG);
        semistrukturertAdresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2)));
        return semistrukturertAdresse;
    }
}
