package no.nav.melosys.service.dokument.sed;

import java.time.LocalDate;
import java.util.*;

import com.google.common.collect.Sets;
import io.getunleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Vedlegg;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.UtpekingAvvisDto;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedPdfData;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagUtenSoknad;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EessiServiceTest {
    @Mock
    private SedDataBygger sedDataBygger;
    @Mock
    private EessiConsumer eessiConsumer;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private SedDataGrunnlagFactory dokumentdataGrunnlagFactory;

    private EessiService eessiService;

    private static final long BEHANDLING_ID = 1L;

    @Captor
    private ArgumentCaptor<SedDataDto> sedDataDtoCaptor;

    private final EasyRandom easyRandom = new EasyRandom();
    private final FakeUnleash unleash = new FakeUnleash();
    private final String mottakerBelgia1 = "BE:12222";
    private final String mottakerBelgia2 = "BE:9999";
    private final String mottakerBelgia3 = "BE:123131";
    private final String mottakerTyskland1 = "DE:4444";
    private final String mottakerTyskland2 = "DE:9999";

    private final Institusjon institusjonBelgia1 = new Institusjon(mottakerBelgia1, null, Landkoder.BE.getKode());
    private final Institusjon institusjonBelgia2 = new Institusjon(mottakerBelgia2, null, Landkoder.BE.getKode());
    private final Institusjon institusjonBelgia3 = new Institusjon(mottakerBelgia3, null, Landkoder.BE.getKode());
    private final Institusjon institusjonTyskland1 = new Institusjon(mottakerTyskland1, null, Landkoder.DE.getKode());
    private final Institusjon institusjonTyskland2 = new Institusjon(mottakerTyskland2, null, Landkoder.DE.getKode());

    @BeforeEach
    void setup() {
        eessiService = new EessiService(behandlingService, behandlingsresultatService, eessiConsumer, joarkFasade,
            sedDataBygger, dokumentdataGrunnlagFactory, unleash);
    }

    private static Behandling lagBehandling() {
        return BehandlingTestFactory.builderWithDefaults()
            .medId(BEHANDLING_ID)
            .medFagsak(FagsakTestFactory.builder().medGsakSaksnummer().build())
            .build();
    }

    private void mockBehandling() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)).thenReturn(lagBehandling());
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Land_iso2.SK);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));

        return behandlingsresultat;
    }

    private void mockBehandlingsresultat() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(lagBehandlingsresultat());
    }

    @Test
    void lagEessiVedlegg() {
        final Journalpost journalpost = lagJournalpost(List.of(lagArkivDokument("1"), lagArkivDokument("2")));
        final String journalpostID = journalpost.getJournalpostId();
        DokumentReferanse dokumentReferanse = new DokumentReferanse(journalpostID, "2");
        when(joarkFasade.hentJournalposterTilknyttetSak(any())).thenReturn(List.of(journalpost));
        when(joarkFasade.hentDokument(anyString(), anyString())).thenReturn(new byte[8]);
        Fagsak fagsak = FagsakTestFactory.builder().medGsakSaksnummer().build();

        Collection<Vedlegg> vedlegg = eessiService.lagEessiVedlegg(fagsak, Set.of(dokumentReferanse));

        assertThat(vedlegg.iterator().next().getInnhold()).hasSize(8);
        assertThat(vedlegg.iterator().next().getTittel()).isEqualTo("Tittel 2");
    }

    @Test
    void opprettOgSendSed_buc03_ingenMedlemsperiodeType() {
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();
        mockBehandlingsresultat();

        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_03, null, "fritekst");
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(lagBehandlingsresultat()), eq(PeriodeType.INGEN));
        verify(eessiConsumer).opprettBucOgSed(sedDataDtoCaptor.capture(), any(), eq(BucType.LA_BUC_03), eq(true), eq(true));
        assertThat(sedDataDtoCaptor.getValue().getYtterligereInformasjon()).isEqualTo("fritekst");
    }

    @Test
    void opprettOgSendSed_buc01_medlemsperiodeTypeAnmodningsperiode() {
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();
        mockBehandlingsresultat();

        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_01, null, null);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(lagBehandlingsresultat()), eq(PeriodeType.ANMODNINGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_01), eq(true), eq(true));
    }

    @Test
    void opprettOgSendSed_a001MedStorbritanniaKonv_mapperKorrektYtterligereInformasjon() {
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();
        var behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.hentAnmodningsperiode().setBestemmelse(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_01, null, "fritekst");


        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(lagBehandlingsresultat()), eq(PeriodeType.ANMODNINGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(sedDataDtoCaptor.capture(), any(), eq(BucType.LA_BUC_01), eq(true), eq(true));
        assertThat(sedDataDtoCaptor.getValue().getYtterligereInformasjon()).isEqualTo("Issued under the EEA EFTA Convention. fritekst");
    }

    @Test
    void opprettOgSendSed_a009MedStorbritanniaKonv_mapperKorrektYtterligereInformasjon() {
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();
        var behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.hentLovvalgsperiode().setBestemmelse(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_04, null, "fritekst");


        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(lagBehandlingsresultat()), eq(PeriodeType.LOVVALGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(sedDataDtoCaptor.capture(), any(), eq(BucType.LA_BUC_04), eq(true), eq(true));
        assertThat(sedDataDtoCaptor.getValue().getYtterligereInformasjon()).isEqualTo("Issued under the EEA EFTA Convention. fritekst");
    }

    @Test
    void opprettOgSendSed_buc02IngenUtpekingsperiode_medlemsperiodeTypeLovvalgsperiode() {
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();
        mockBehandlingsresultat();

        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_02, null, null);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(lagBehandlingsresultat()), eq(PeriodeType.LOVVALGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_02), eq(true), eq(true));
    }

    @Test
    void opprettOgSendSed_buc02MedUtpekingsperiode_medlemsperiodeTypeUtpekingsperiode() {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();

        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_02, null, null);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(behandlingsresultat), eq(PeriodeType.UTPEKINGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_02), eq(true), eq(true));
    }

    @Test
    void opprettOgSendSed_buc04_medlemsperiodeTypeLovvalgsperiode() {
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();
        mockBehandlingsresultat();

        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_04, null, null);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(lagBehandlingsresultat()), eq(PeriodeType.LOVVALGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_04), eq(true), eq(true));
    }

    @Test
    void opprettBucOgSed_verifiserKorrektSedType() {
        OpprettSedDto opprettSedDto = new OpprettSedDto();
        opprettSedDto.setRinaUrl("localhost:3000");
        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)).thenReturn(lagBehandling());
        when(eessiConsumer.opprettBucOgSed(any(SedDataDto.class), any(), any(BucType.class), anyBoolean(), anyBoolean())).thenReturn(opprettSedDto);
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        mockBehandlingsresultat();

        eessiService.opprettBucOgSed(BEHANDLING_ID, BucType.LA_BUC_01, List.of(mottakerBelgia1), Collections.emptyList());
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), anyCollection(), eq(BucType.LA_BUC_01), eq(false), eq(false));
    }

    @Test
    void hentMottakerinstitusjoner_forventListeMedRettType() {
        when(eessiConsumer.hentMottakerinstitusjoner(anyString(), anyList())).thenReturn(Arrays.asList(
            new Institusjon("1", "Test1", "NO"),
            new Institusjon("2", "Test2", "NO")
        ));

        List<Institusjon> mottakerinstitusjoner = eessiService.hentEessiMottakerinstitusjoner("LA_BUC_01", List.of("FR"));

        verify(eessiConsumer).hentMottakerinstitusjoner(anyString(), anyList());
        assertThat(mottakerinstitusjoner).hasSize(2).hasOnlyElementsOfType(Institusjon.class);
    }

    @Test
    void hentTilknyttedeBucer_forventListeMedRettType() {
        when(eessiConsumer.hentTilknyttedeBucer(anyLong(), anyList())).thenReturn(Arrays.asList(
            easyRandom.nextObject(BucInformasjon.class),
            easyRandom.nextObject(BucInformasjon.class),
            easyRandom.nextObject(BucInformasjon.class)
        ));

        List<BucInformasjon> tilknyttedeBucer = eessiService.hentTilknyttedeBucer(123L, Arrays.asList("utkast", "sendt"));

        verify(eessiConsumer).hentTilknyttedeBucer(anyLong(), anyList());
        assertThat(tilknyttedeBucer).hasSize(3).hasOnlyElementsOfType(BucInformasjon.class);
    }

    @Test
    void hentTilknyttedeBucer_medFeilIConsumer_forventException() {
        when(eessiConsumer.hentTilknyttedeBucer(anyLong(), anyList())).thenThrow(new IntegrasjonException("Error!"));
        assertThatExceptionOfType(IntegrasjonException.class)
            .isThrownBy(() ->
                eessiService.hentTilknyttedeBucer(123L, Collections.singletonList("utkast")));
    }

    @Test
    void støtterAutomatiskBehandling_verifiserA001A003A009A010støtterAutomatiskBehandling() {
        List<String> sedTyperAutomatiskBehandling = Arrays.asList(
            SedType.A001.name(),
            SedType.A009.name(),
            SedType.A010.name()
        );

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setLovvalgsland(Landkoder.DE.name());
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID("123")).thenReturn(melosysEessiMelding);

        for (String sedType : sedTyperAutomatiskBehandling) {
            melosysEessiMelding.setSedType(sedType);
            assertThat(eessiService.støtterAutomatiskBehandling("123")).isTrue();
        }
    }

    @Test
    void støtterAutomatiskBehandling_verifiserStøtterIkkeAutomatiskBehandling() {
        List<String> sedTyperIkkeAutomatiskBehandling = Arrays.asList(
            SedType.H001.name(),
            SedType.A002.name(),
            SedType.A004.name(),
            SedType.A005.name(),
            SedType.A006.name(),
            SedType.A007.name(),
            SedType.A008.name(),
            SedType.A011.name(),
            SedType.A012.name()
        );

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setLovvalgsland(Landkoder.DE.name());
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID("123")).thenReturn(melosysEessiMelding);

        for (String sedType : sedTyperIkkeAutomatiskBehandling) {
            melosysEessiMelding.setSedType(sedType);
            assertThat(eessiService.støtterAutomatiskBehandling("123")).isFalse();
        }
    }

    @Test
    void støtterAutomatiskBehandling_nullVerdi_forventFalse() {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType(null);
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID("123")).thenReturn(melosysEessiMelding);
        assertThat(eessiService.støtterAutomatiskBehandling("123")).isFalse();
    }

    @Test
    void støtterAutomatiskBehandling_a003ikkeUtpekt_verifiserStøtterAutomatiskBehandling() {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setLovvalgsland(Landkoder.SE.name());
        melosysEessiMelding.setSedType("A003");
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID("123")).thenReturn(melosysEessiMelding);
        assertThat(eessiService.støtterAutomatiskBehandling("123")).isTrue();
    }

    @Test
    void støtterAutomatiskBehandling_a003erUtpekt_verifiserStøtterIkkeAutomatiskBehandling() {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.name());
        melosysEessiMelding.setSedType("A003");
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID("123")).thenReturn(melosysEessiMelding);
        assertThat(eessiService.støtterAutomatiskBehandling("123")).isTrue();
    }

    @Test
    void hentSakForRinaSaksnummer_forventOptionalIkkePresent() {
        when(eessiConsumer.hentSakForRinasaksnummer(anyString()))
            .thenReturn(Collections.emptyList());
        Optional<Long> res = eessiService.finnSakForRinasaksnummer("123");
        assertThat(res).isNotPresent();
    }

    @Test
    void hentSakForRinaSaksnummer_forventOptionalPresent() {
        when(eessiConsumer.hentSakForRinasaksnummer(anyString()))
            .thenReturn(Collections.singletonList(new SaksrelasjonDto(123L, "123", "123")));
        Optional<Long> res = eessiService.finnSakForRinasaksnummer("123");
        assertThat(res).isPresent();
    }

    @Test
    void lagreSaksrelasjon_validerInput() {
        eessiService.lagreSaksrelasjon(123L, "123", "312");
        verify(eessiConsumer).lagreSaksrelasjon(any());
    }

    @Test
    void sendAnmodningUnntakSvar_forventKall() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(BEHANDLING_ID)
            .build();

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(new SedDokument());
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));

        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(dokumentdataGrunnlagFactory.av(any(), any())).thenReturn(Mockito.mock(SedDataGrunnlagUtenSoknad.class));
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        mockBehandlingsresultat();


        eessiService.sendAnmodningUnntakSvar(BEHANDLING_ID, null);


        verify(behandlingService).hentBehandlingMedSaksopplysninger(BEHANDLING_ID);
        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(PeriodeType.ANMODNINGSPERIODE));
        verify(dokumentdataGrunnlagFactory).av(any(), any());
        verify(eessiConsumer).sendSedPåEksisterendeBuc(any(SedDataDto.class), any(), eq(SedType.A002));
    }

    @Test
    void sendGodkjenningArbeidFlereLand() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(BEHANDLING_ID)
            .medFagsak(FagsakTestFactory.builder().medGsakSaksnummer().build())
            .build();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(new SedDokument());
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));

        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(dokumentdataGrunnlagFactory.av(any(), any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        mockBehandlingsresultat();


        eessiService.sendGodkjenningArbeidFlereLand(BEHANDLING_ID, null);


        verify(behandlingService).hentBehandlingMedSaksopplysninger(BEHANDLING_ID);
        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(PeriodeType.LOVVALGSPERIODE));
        verify(dokumentdataGrunnlagFactory).av(any(), any());
        verify(eessiConsumer).sendSedPåEksisterendeBuc(any(SedDataDto.class), any(), eq(SedType.A012));
    }

    @Test
    void sendGodkjenningArbeidFlereLand__feiler_ikke_når_x008_utsending_feiler() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults().build();
        behandling.setId(BEHANDLING_ID);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        Fagsak fagsak = FagsakTestFactory.builder().medGsakSaksnummer().build();
        behandling.setFagsak(fagsak);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(new SedDokument());
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));

        SedInformasjon sedInformasjon = new SedInformasjon("1", "2", LocalDate.now(), LocalDate.now(), "A012", "whatever", null);
        BucInformasjon bucInformasjon = new BucInformasjon("1", true, "LA_BUC_02", LocalDate.now(), null, List.of(sedInformasjon));
        List<BucInformasjon> bucInformasjonListe = List.of(bucInformasjon);

        when(eessiConsumer.hentTilknyttedeBucer(eq(FagsakTestFactory.GSAK_SAKSNUMMER), any())).thenReturn(bucInformasjonListe);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(dokumentdataGrunnlagFactory.av(any(), any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        mockBehandlingsresultat();
        doThrow(RuntimeException.class).when(eessiConsumer).sendSedPåEksisterendeBuc(any(), any(), eq(SedType.X008));


        eessiService.sendGodkjenningArbeidFlereLand(BEHANDLING_ID, null);


        verify(behandlingService, times(2)).hentBehandlingMedSaksopplysninger(BEHANDLING_ID);
        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(PeriodeType.LOVVALGSPERIODE));
        verify(dokumentdataGrunnlagFactory, times(2)).av(any(), any());
        verify(eessiConsumer).sendSedPåEksisterendeBuc(any(SedDataDto.class), any(), eq(SedType.A012));
    }

    @Test
    void sendAvslagUtpekingSvar__feiler_ikke_når_x008_utsending_feiler() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults().build();
        behandling.setId(BEHANDLING_ID);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        Fagsak fagsak = FagsakTestFactory.builder().medGsakSaksnummer().build();
        behandling.setFagsak(fagsak);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(new SedDokument());
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));

        SedInformasjon sedInformasjon = new SedInformasjon("1", "2", LocalDate.now(), LocalDate.now(), "A012", "whatever", null);
        BucInformasjon bucInformasjon = new BucInformasjon("1", true, "LA_BUC_02", LocalDate.now(), null, List.of(sedInformasjon));
        List<BucInformasjon> bucInformasjonListe = List.of(bucInformasjon);

        when(eessiConsumer.hentTilknyttedeBucer(eq(FagsakTestFactory.GSAK_SAKSNUMMER), any())).thenReturn(bucInformasjonListe);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        when(sedDataBygger.lagUtkast(any(), any(), any())).thenReturn(new SedDataDto());
        mockBehandlingsresultat();
        doThrow(RuntimeException.class).when(eessiConsumer).sendSedPåEksisterendeBuc(any(), any(), eq(SedType.X008));


        UtpekingAvvis utpekingAvvis = new UtpekingAvvis();
        utpekingAvvis.setEtterspørInformasjon(false);

        eessiService.sendAvslagUtpekingSvar(BEHANDLING_ID, utpekingAvvis);


        verify(behandlingService, times(2)).hentBehandlingMedSaksopplysninger(BEHANDLING_ID);
        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(PeriodeType.LOVVALGSPERIODE));
        verify(dokumentdataGrunnlagFactory, times(1)).av(any(), any());
        verify(eessiConsumer).sendSedPåEksisterendeBuc(any(SedDataDto.class), any(), eq(SedType.A004));
    }

    @Test
    void genererSedPdf_sedA001_medlemsperiodeTypeAnmodningsperiode() {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandlingsresultat();

        byte[] pdf = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A001);

        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(PeriodeType.ANMODNINGSPERIODE));
        verify(eessiConsumer).genererSedPdf(any(), any());
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    void genererSedPdf_sedA001_storbritanniaKonvFårTilpassetYtterligereInformasjon() {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        var sedPdfData = new SedPdfData();
        sedPdfData.setFritekst("fritekst");
        var behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.hentAnmodningsperiode().setBestemmelse(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        byte[] pdf = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A001, sedPdfData);


        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(PeriodeType.ANMODNINGSPERIODE));
        verify(eessiConsumer).genererSedPdf(sedDataDtoCaptor.capture(), any());
        assertThat(sedDataDtoCaptor.getValue().getYtterligereInformasjon()).isEqualTo("Issued under the EEA EFTA Convention. fritekst");
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    void genererSedPdf_sedA003MedUtpekingsperiode_medlemsperiodeTypeUtpekingsperiode() {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));

        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        byte[] pdf = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A003);

        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(PeriodeType.UTPEKINGSPERIODE));
        verify(eessiConsumer).genererSedPdf(any(), any());
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    void genererSedPdf_sedA009_medlemsperiodeTypeLovvalgsperiode() {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));

        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        byte[] pdf = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A009);

        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(PeriodeType.LOVVALGSPERIODE));
        verify(eessiConsumer).genererSedPdf(any(), any());
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    void genererSedForhåndsvisning_medSedPdfData_verifiserSedDataDtoPreutfylt() {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(PeriodeType.class))).thenReturn(new SedDataDto());
        mockBehandlingsresultat();
        mockBehandling();

        SedPdfData sedPdfData = new SedPdfData();
        sedPdfData.setVilSendeAnmodningOmMerInformasjon(null);
        sedPdfData.setNyttLovvalgsland("SE");
        byte[] pdf = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A001, sedPdfData);

        verify(behandlingService).hentBehandlingMedSaksopplysninger(BEHANDLING_ID);
        verify(dokumentdataGrunnlagFactory).av(any());
        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(PeriodeType.ANMODNINGSPERIODE));
        verify(eessiConsumer).genererSedPdf(sedDataDtoCaptor.capture(), any());
        assertThat(pdf).isEqualTo(PDF);

        SedDataDto sedDataDto = sedDataDtoCaptor.getValue();
        assertThat(sedDataDto.getUtpekingAvvis()).isNotNull()
            .extracting(UtpekingAvvisDto::getNyttLovvalgsland)
            .isEqualTo(sedPdfData.getNyttLovvalgsland());
    }

    @Test
    void hentSedTypeForAnmodningUnntakSvar_forventA002() {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.hentAnmodningsperiode()
            .getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        SedType sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(BEHANDLING_ID);

        assertThat(sedType).isEqualTo(SedType.A002);
    }

    @Test
    void hentSedTypeForAnmodningUnntakSvar_forventA011() {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.hentAnmodningsperiode()
            .getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        SedType sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(BEHANDLING_ID);

        assertThat(sedType).isEqualTo(SedType.A011);
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toMottakereToMottakerLandMottakereKorrektSatt_returnererMottakerInstitusjoner() {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Land_iso2> mottakerLand = List.of(Land_iso2.BE, Land_iso2.DE);

        final Set<String> valgteMottakerInstitusjoner = Set.of(mottakerBelgia1, mottakerTyskland1);

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Set.of(Land_iso2.BE.getKode(), Land_iso2.DE.getKode())))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia2, institusjonTyskland1, institusjonTyskland2));

        Set<String> avklarteMottakerInstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
        verify(eessiConsumer).hentMottakerinstitusjoner(eq(bucType.name()), anySet());
        assertThat(avklarteMottakerInstitusjoner).isEqualTo(valgteMottakerInstitusjoner);
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toMottakereSisteErIkkeEessiReady_returnererTomListe() {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Land_iso2> mottakerLand = List.of(Land_iso2.BE, Land_iso2.DE);

        final Set<String> valgteMottakerInstitusjoner = Set.of(mottakerBelgia1, mottakerTyskland1);

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Set.of(Land_iso2.BE.getKode(), Land_iso2.DE.getKode())))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia2));

        Set<String> avklarteMottakerInstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
        verify(eessiConsumer).hentMottakerinstitusjoner(eq(bucType.name()), anySet());
        assertThat(avklarteMottakerInstitusjoner).isEmpty();
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toLandInstitusjonManglerForSiste_kasterException() {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Land_iso2> mottakerLand = List.of(Land_iso2.BE, Land_iso2.DE);

        final Set<String> valgteMottakerInstitusjoner = Set.of(mottakerBelgia1);

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Set.of(Landkoder.BE.getKode(), Landkoder.DE.getKode())))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia2, institusjonTyskland1, institusjonTyskland2));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() ->
                eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType))
            .withMessageContaining("Finner ingen gyldig mottakerinstitusjon for arbeidsland Tyskland");
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toLandInstitusjonManglerForSiste2_kasterException() {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Land_iso2> mottakerLand = List.of(Land_iso2.BE, Land_iso2.DE);

        final Set<String> valgteMottakerInstitusjoner = Set.of(mottakerBelgia1, mottakerBelgia3, mottakerTyskland1);

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Set.of(Landkoder.BE.getKode(), Landkoder.DE.getKode())))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia3, institusjonBelgia2, institusjonTyskland1, institusjonTyskland2));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType))
            .withMessageContaining("Kan kun velge en mottakerinstitusjon per land. Validerte mottakere:");
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toLandErPåkobletIngenInstitusjonValgt_kasterException() {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Land_iso2> mottakerLand = List.of(Land_iso2.BE, Land_iso2.DE);

        final Set<String> valgteMottakerInstitusjoner = Collections.emptySet();

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Set.of(Landkoder.BE.getKode(), Landkoder.DE.getKode())))
            .thenReturn(List.of(institusjonBelgia2, institusjonTyskland2));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() ->
                eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType))
            .withMessageContaining(
                "Finner ingen gyldig mottakerinstitusjon for arbeidsland " + Landkoder.BE.getBeskrivelse() + System.lineSeparator() +
                    "Finner ingen gyldig mottakerinstitusjon for arbeidsland " + Landkoder.DE.getBeskrivelse());
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toLandEnErIkkePåkobletIngenInstitusjonValgt_returnererTomListe() {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Land_iso2> mottakerLand = List.of(Land_iso2.BE, Land_iso2.DE);

        final Set<String> valgteMottakerInstitusjoner = Collections.emptySet();

        final Institusjon institusjonBelgia = new Institusjon("BE:44444", null, Landkoder.BE.getKode());

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Set.of(Landkoder.BE.getKode(), Landkoder.DE.getKode())))
            .thenReturn(List.of(institusjonBelgia));

        Set<String> avklarteMottakere = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
        assertThat(avklarteMottakere).isEmpty();
    }

    @Test
    void landErEessiReady_toLandEtErEessiReady_forventFalse() {
        final BucType bucType = BucType.LA_BUC_01;
        final List<Land_iso2> land = List.of(Land_iso2.SE, Land_iso2.DK);

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Set.of(Land_iso2.SE.getKode())))
            .thenReturn(List.of(new Institusjon("2", "", "")));

        assertThat(eessiService.landErEessiReady(bucType.name(), land)).isFalse();
    }

    @Test
    void landErEessiReady_toLandAlleErEessiReady_forventTrue() {
        final BucType bucType = BucType.LA_BUC_01;
        final List<Land_iso2> land = List.of(Land_iso2.SE, Land_iso2.DK);

        when(eessiConsumer.hentMottakerinstitusjoner(eq(bucType.name()), any()))
            .thenReturn(List.of(new Institusjon("2", "", "")));

        assertThat(eessiService.landErEessiReady(bucType.name(), land)).isTrue();
    }

    @Test
    void kanOppretteSedPåBuc_fårCreateTilbake_true() {
        when(eessiConsumer.hentMuligeAksjoner("5566")).thenReturn(Collections.singletonList("5fcffb7e6a9640acac5a09abc5a08ef6 A004 Create"));

        assertThat(eessiService.kanOppretteSedTyperPåBuc("5566", SedType.A004)).isTrue();
    }

    @Test
    void kanOppretteSedPåBuc_fårCreateTilbakePåFeilSed_false() {
        when(eessiConsumer.hentMuligeAksjoner("5566")).thenReturn(Collections.singletonList("5fcffb7e6a9640acac5a09abc5a08ef6 A004 Create"));

        assertThat(eessiService.kanOppretteSedTyperPåBuc("5566", SedType.A011)).isFalse();
    }

    @Test
    void kanOppretteSedPåBuc_tomListe_false() {
        when(eessiConsumer.hentMuligeAksjoner("5566")).thenReturn(Collections.emptyList());

        assertThat(eessiService.kanOppretteSedTyperPåBuc("5566", SedType.A011)).isFalse();
    }

    private static Journalpost lagJournalpost(List<ArkivDokument> dokumenter) {
        Journalpost journalpost = new Journalpost("jpID");
        journalpost.setHoveddokument(dokumenter.get(0));
        journalpost.getVedleggListe().clear();
        journalpost.getVedleggListe().addAll(dokumenter.subList(1, dokumenter.size()));
        return journalpost;
    }

    private static ArkivDokument lagArkivDokument(String dokumentID) {
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setDokumentId(dokumentID);
        arkivDokument.setTittel("Tittel " + dokumentID);
        return arkivDokument;
    }
}
