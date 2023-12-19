package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Nyvurderingbakgrunner;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.*;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Innvilgelse;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Person;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoVirksomhet;
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrl;
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.attest.*;
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.innvilgelse.InnvilgelseTrygdeavtale;
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.innvilgelse.Soknad;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.melosys.domain.kodeverk.Mottakerroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.dokument.DokgenTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokgenMalMapperTest {
    public static final String FORRETNINGSADRESSE_ORG = "Storgata 1";
    public static final LocalDate SOKNADSDATO = LocalDate.of(2000, 1, 1);
    public static final LocalDate FØDSELSDATO = LocalDate.of(2000, 1, 1);

    @Mock
    private InnvilgelseFtrlMapper mockInnvilgelseFtrlMapper;
    @Mock
    private DokgenMapperDatahenter mockDokgenMapperDatahenter;
    @Mock
    private TrygdeavtaleMapper mockTrygdeavtaleMapper;

    private DokgenMalMapper dokgenMalMapper;

    @BeforeEach
    void init() {
        dokgenMalMapper = new DokgenMalMapper(
            mockDokgenMapperDatahenter,
            mockInnvilgelseFtrlMapper,
            mockTrygdeavtaleMapper);
    }

    @Test
    void feilerNårProduserbartDokumentIkkeErStøttet() {
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(ATTEST_A1)
            .medBehandling(lagBehandling())
            .build();

        assertThatThrownBy(() -> dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(BRUKER)))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("ProduserbartDokument ATTEST_A1 er ikke støttet av melosys-dokgen");
    }

    @Test
    void skalMappeMedBrukerAdressePDL() {
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(
            PersonopplysningerObjectFactory.lagDonaldDuckPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(
            PersonopplysningerObjectFactory.lagDonaldDuckPersondata());

        Behandling behandling = lagBehandling();

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(BRUKER));

        assertThat((SaksbehandlingstidSoknad) dokgenDto)
            .extracting(
                dto -> dto.getSaksinfo().navnBruker(),
                dto -> dto.getMottaker().navn(),
                dto -> dto.getMottaker().postnr(),
                dto -> dto.getMottaker().poststed(),
                dto -> dto.getMottaker().region()
            ).containsExactly(
                "Duck Donald",
                "Duck Donald",
                POSTNR_BRUKER,
                POSTSTED_BRUKER,
                REGION
            );
    }

    @Test
    void skalMappeMedBrukerAdresse() {
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());

        Behandling behandling = lagBehandling();

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(BRUKER));

        assertThat((SaksbehandlingstidSoknad) dokgenDto)
            .extracting(
                dto -> dto.getSaksinfo().navnBruker(),
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
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());

        Behandling behandling = lagBehandling();

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(BRUKER));

        assertThat((SaksbehandlingstidSoknad) dokgenDto)
            .extracting(
                dto -> dto.getSaksinfo().navnBruker(),
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
    void mapping_avsenderMyndighet_ok() {
        when(mockDokgenMapperDatahenter.hentLandnavnFraLandkode("FI")).thenReturn("Finland");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());

        Behandling behandling = lagBehandling();
        LocalDate forsendelseMottattDato = LocalDate.of(2022, 1, 19);
        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medForsendelseMottatt(forsendelseMottattDato.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant())
            .medAvsendertype(Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET)
            .medAvsenderLand("FI")
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(BRUKER));

        assertThat(dokgenDto)
            .isInstanceOf(SaksbehandlingstidSoknad.class);

        assertThat((SaksbehandlingstidSoknad) dokgenDto)
            .extracting(
                SaksbehandlingstidSoknad::getAvsenderTypeSoknad,
                SaksbehandlingstidSoknad::getAvsenderLand,
                SaksbehandlingstidSoknad::getDatoBehandlingstid
            ).containsExactly(
                UTENLANDSK_TRYGDEMYNDIGHET,
                "Finland",
                forsendelseMottattDato.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant().plus(Period.ofWeeks(Saksbehandlingstid.SAKSBEHANDLINGSTID_UKER))
            );
    }

    @Test
    void skalMappeMedFullmektigAdresse() {
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());

        Behandling behandling = lagBehandling();

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(ARBEIDSGIVER));

        assertThat((SaksbehandlingstidSoknad) dokgenDto)
            .extracting(
                dto -> dto.getSaksinfo().navnBruker(),
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
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());

        Behandling behandling = lagBehandling();

        OrganisasjonDokument org = lagOrg();
        org.getOrganisasjonDetaljer().setForretningsadresse(singletonList(lagOrgForretningsadresse()));
        org.getOrganisasjonDetaljer().setPostadresse(emptyList());

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medOrg(org)
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottakerRepresentant(Aktoertype.ORGANISASJON));

        assertThat((SaksbehandlingstidSoknad) dokgenDto)
            .extracting(
                dto -> dto.getSaksinfo().navnBruker(),
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
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());

        Behandling behandling = lagBehandling();

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottakerRepresentant(Aktoertype.ORGANISASJON));

        assertThat((SaksbehandlingstidSoknad) dokgenDto)
            .extracting(
                dto -> dto.getSaksinfo().navnBruker(),
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
        when(mockDokgenMapperDatahenter.hentNorskPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medBehandling(behandling)
            .medOrg(lagOrg(Landkoder.NO))
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .medManglerInfoFritekst("Dummy")
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(BRUKER));

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
        when(mockDokgenMapperDatahenter.hentNorskPoststed(any())).thenReturn("Andeby");
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentFullmektigNavn(any(), eq(Representerer.BRUKER))).thenReturn("Fullmektig AS");

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_ARBEIDSGIVER)
            .medBehandling(behandling)
            .medOrg(lagOrg(Landkoder.NO))
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .medManglerInfoFritekst("Dummy")
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(ARBEIDSGIVER));

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
    void skalMappeFtrlInnvilgelsesbrevTilBruker() {
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());
        when(mockInnvilgelseFtrlMapper.map(any())).thenReturn(lagInnvilgelseFtrl());

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new InnvilgelseFtrlBrevbestilling.Builder()
            .medProduserbartdokument(INNVILGELSE_FOLKETRYGDLOVEN)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .build();

        assertThat(dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(BRUKER)))
            .isInstanceOf(InnvilgelseFtrl.class);
    }

    @Test
    void skalMappevEdtakOpphørtMedlemskapTilBruker() {
        var behandlingsResultat = new Behandlingsresultat();
        behandlingsResultat.setMedlemAvFolketrygden(new MedlemAvFolketrygden());
        var medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setFom(LocalDate.now().minusMonths(1));
        medlemskapsperiode.setTom(LocalDate.now());
        medlemskapsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsResultat.getMedlemAvFolketrygden().setMedlemskapsperioder(List.of(medlemskapsperiode));
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsResultat);

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new VedtakOpphoertMedlemskapBrevbestilling.Builder()
            .medProduserbartdokument(VEDTAK_OPPHOERT_MEDLEMSKAP)
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medOpphoertBegrunnelseFritekst("Dummy")
            .build();


        assertThat(dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(BRUKER)))
            .isInstanceOf(VedtakOpphoertMedlemskap.class);
    }

    @Test
    void skalMappeStorbritanniabrev() {
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());
        when(mockTrygdeavtaleMapper.map(any(), eq(Land_iso2.GB))).thenReturn(lagInnvilgelseOgAttestStorbritannia());

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new InnvilgelseBrevbestilling.Builder()
            .medProduserbartdokument(TRYGDEAVTALE_GB)
            .medBehandling(behandling)
            .medOrg(lagOrg(Landkoder.GB))
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(BRUKER));
        assertThat(dokgenDto).isInstanceOf(InnvilgelseOgAttestTrygdeavtale.class);

        InnvilgelseTrygdeavtale innvilgelse = ((InnvilgelseOgAttestTrygdeavtale) dokgenDto).getInnvilgelse();
        AttestTrygdeavtale attest = ((InnvilgelseOgAttestTrygdeavtale) dokgenDto).getAttest();

        assertThat(innvilgelse).extracting(
            i -> i.getInnvilgelse().innledningFritekst(),
            InnvilgelseTrygdeavtale::getArtikkel,
            i -> i.getSoknad().virksomhetsnavn(),
            InnvilgelseTrygdeavtale::isVirksomhetArbeidsgiverSkalHaKopi
        ).containsExactly(
            "Innledning",
            Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1,
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
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1,
                "Kone",
                List.of(),
                "Mrs. London"
            );

        assertThat(((InnvilgelseOgAttestTrygdeavtale) dokgenDto).getNyVurderingBakgrunn())
            .isEqualTo(Nyvurderingbakgrunner.NYE_OPPLYSNINGER.getKode());
        assertThat(((InnvilgelseOgAttestTrygdeavtale) dokgenDto).isSkalHaInfoOmRettigheter())
            .isFalse();
    }

    @Test
    void skalMappeFritekstbrevTilBruker() {
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());
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

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(BRUKER));
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

        assertThat(dokgenDto.getMottaker().type()).isEqualTo(BRUKER.getKode());
    }

    @Test
    void skalMappeFritekstbrevTilVirksomhet() {
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(null);
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(null);

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_VIRKSOMHET)
            .medBehandling(behandling)
            .medFritekstTittel("Min tittel")
            .medFritekst("Innhold")
            .medOrg(lagOrg())
            .medSaksbehandlerNavn("Fetter Anton")
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(VIRKSOMHET));
        assertThat(dokgenDto).isInstanceOf(Fritekstbrev.class);

        assertThat((Fritekstbrev) dokgenDto)
            .extracting(
                Fritekstbrev::getFritekstTittel,
                Fritekstbrev::getFritekst,
                Fritekstbrev::getNavnFullmektig,
                Fritekstbrev::getSaksbehandlerNavn
            ).containsExactly(
                "Min tittel",
                "Innhold",
                null,
                "Fetter Anton"
            );

        assertThat((SaksinfoVirksomhet) dokgenDto.getSaksinfo())
            .extracting(
                SaksinfoVirksomhet::saksnummer,
                SaksinfoVirksomhet::navnVirksomhet,
                SaksinfoVirksomhet::orgnr)
            .containsExactly(
                SAKSNUMMER,
                NAVN_ORG,
                ORGNR
            );

        assertThat(dokgenDto.getMottaker().type()).isEqualTo(VIRKSOMHET.getKode());
    }

    @Test
    void skalMappeFritekstbrevTilArbeidsgiver() {
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentFullmektigNavn(any(), eq(Representerer.ARBEIDSGIVER))).thenReturn(null);

        Behandling behandling = lagBehandling(lagFagsak(true));

        DokgenBrevbestilling brevbestilling = new FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_ARBEIDSGIVER)
            .medBehandling(behandling)
            .medFritekstTittel("Min tittel")
            .medFritekst("Innhold")
            .medOrg(lagOrg())
            .medKontaktopplysninger(true)
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(ARBEIDSGIVER));
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

        assertThat(dokgenDto.getMottaker().type()).isEqualTo(ARBEIDSGIVER.getKode());
    }

    @Test
    void skalMappeAvslagsbrevPgaManglendeOpplysningerTilBruker() {
        when(mockDokgenMapperDatahenter.hentPersondata(any())).thenReturn(lagPersondata());
        when(mockDokgenMapperDatahenter.hentPersonMottaker(any())).thenReturn(lagPersondata());
        Behandling behandling = lagBehandling(lagFagsak(true));
        DokgenBrevbestilling brevbestilling = new AvslagBrevbestilling.Builder()
            .medProduserbartdokument(AVSLAG_MANGLENDE_OPPLYSNINGER)
            .medBehandling(behandling)
            .build();

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling, lagMottaker(BRUKER));

        assertThat(dokgenDto.getMottaker().type()).isEqualTo(BRUKER.getKode());
    }

    private InnvilgelseBrevbestilling lagInnvilgelseBrevbestilling() {
        return new InnvilgelseBrevbestilling.Builder()
            .medInnledningFritekst("Innledning")
            .medBehandling(lagBehandling())
            .medPersonDokument(lagPersondata())
            .medPersonMottaker(lagPersondata())
            .build();
    }

    private InnvilgelseFtrl lagInnvilgelseFtrl() {
        return new InnvilgelseFtrl.Builder(lagInnvilgelseFtrlBrevbestilling())
            .avgiftsperioder(emptyList())
            .medlemskapsperioder(emptyList())
            .bestemmelse(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8)
            .avslåttHelsedelFørMottaksdato(false)
            .trygdeavgiftMottaker(Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV)
            .skatteplikttype(Skatteplikttype.SKATTEPLIKTIG)
            .ftrl_2_8_begrunnelse(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_MULTINASJONALT_SELSKAP)
            .begrunnelseAnnenGrunnFritekst(null)
            .arbeidsgivere(List.of("Egon Olsen AS"))
            .arbeidsland(Land_iso2.US.getKode())
            .trygdeavtaleMedArbeidsland(false)
            .betalerArbeidsgiveravgift(true)
            .build();
    }

    private InnvilgelseFtrlBrevbestilling lagInnvilgelseFtrlBrevbestilling() {
        return new InnvilgelseFtrlBrevbestilling.Builder()
            .medInnledningFritekst("Innledning")
            .medBegrunnelseFritekst("Begrunnelse")
            .medTrygdeavgiftFritekst("Trygdeavgift fritekst")
            .medBehandling(lagBehandling())
            .medPersonDokument(lagPersondata())
            .medPersonMottaker(lagPersondata())
            .build();
    }

    private InnvilgelseOgAttestTrygdeavtale lagInnvilgelseOgAttestStorbritannia() {
        return new InnvilgelseOgAttestTrygdeavtale.Builder(lagInnvilgelseBrevbestilling())
            .innvilgelse(lagInnvilgelseStorbritannia())
            .attest(lagAttestStorbritannia())
            .nyVurderingBakgrunn(Nyvurderingbakgrunner.NYE_OPPLYSNINGER.getKode())
            .build();
    }

    private InnvilgelseTrygdeavtale lagInnvilgelseStorbritannia() {
        return new InnvilgelseTrygdeavtale.Builder()
            .innvilgelse(Innvilgelse.av(lagInnvilgelseBrevbestilling()))
            .artikkel(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1)
            .soknad(new Soknad(SOKNADSDATO,
                LOVVALGSPERIODE_FOM,
                LOVVALGSPERIODE_TOM,
                "Virksomhetsnavn",
                Land_iso2.GB.getBeskrivelse()
            ))
            .familie(null)
            .virksomhetArbeidsgiverSkalHaKopi(true)
            .build();
    }

    private AttestTrygdeavtale lagAttestStorbritannia() {
        DokgenBrevbestilling dokgenBrevbestillingBuilder = new DokgenBrevbestilling()
            .toBuilder()
            .medBehandling(lagBehandling())
            .medPersonDokument(lagPersondata())
            .build();
        return new AttestTrygdeavtale.Builder(dokgenBrevbestillingBuilder)
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
            .representant(new RepresentantTrygdeavtale(
                "Mrs. London",
                List.of("UK Street 1337"))
            )
            .utsendelse(new Utsendelse(
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1,
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
        semistrukturertAdresse.setLandkode("NO");
        return semistrukturertAdresse;
    }
}
