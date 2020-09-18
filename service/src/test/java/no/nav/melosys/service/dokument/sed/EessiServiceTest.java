package no.nav.melosys.service.dokument.sed;

import java.util.*;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.UtpekingAvvisDto;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedPdfData;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad;
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
    private SedDataGrunnlagFactory dokumentdataGrunnlagFactory;

    private EessiService eessiService;

    private static final long BEHANDLING_ID = 1L;

    @Captor
    private ArgumentCaptor<SedDataDto> sedDataDtoCaptor;

    private final EasyRandom easyRandom = new EasyRandom();

    @BeforeEach
    public void setup() throws Exception {
        eessiService = new EessiService(sedDataBygger, dokumentdataGrunnlagFactory,
            eessiConsumer, behandlingService, behandlingsresultatService);
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("123");
        return behandling;
    }

    private void mockBehandling() throws IkkeFunnetException {
        when(behandlingService.hentBehandling(eq(BEHANDLING_ID))).thenReturn(lagBehandling());
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.SK);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));

        return behandlingsresultat;
    }

    private void mockBehandlingsresultat() throws IkkeFunnetException {
        when(behandlingsresultatService.hentBehandlingsresultat(eq(BEHANDLING_ID))).thenReturn(lagBehandlingsresultat());
    }

    @Test
    void opprettOgSendSed_buc03_ingenMedlemsperiodeType() throws Exception {
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();
        mockBehandlingsresultat();

        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_03, null, "fritekst");
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(lagBehandlingsresultat()), eq(MedlemsperiodeType.INGEN));
        verify(eessiConsumer).opprettBucOgSed(sedDataDtoCaptor.capture(), any(), eq(BucType.LA_BUC_03), eq(true));
        assertThat(sedDataDtoCaptor.getValue().getYtterligereInformasjon()).isEqualTo("fritekst");
    }

    @Test
    void opprettOgSendSed_buc01_medlemsperiodeTypeAnmodningsperiode() throws Exception {
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();
        mockBehandlingsresultat();

        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_01, null, null);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(lagBehandlingsresultat()), eq(MedlemsperiodeType.ANMODNINGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_01), eq(true));
    }

    @Test
    void opprettOgSendSed_buc02IngenUtpekingsperiode_medlemsperiodeTypeLovvalgsperiode() throws Exception {
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();
        mockBehandlingsresultat();

        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_02, null, null);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(lagBehandlingsresultat()), eq(MedlemsperiodeType.LOVVALGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_02), eq(true));
    }

    @Test
    void opprettOgSendSed_buc02MedUtpekingsperiode_medlemsperiodeTypeUtpekingsperiode() throws Exception {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());
        when(behandlingsresultatService.hentBehandlingsresultat(eq(BEHANDLING_ID))).thenReturn(behandlingsresultat);
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();

        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_02, null, null);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(behandlingsresultat), eq(MedlemsperiodeType.UTPEKINGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_02), eq(true));
    }

    @Test
    void opprettOgSendSed_buc04_medlemsperiodeTypeLovvalgsperiode() throws Exception {
        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true))).thenReturn(new OpprettSedDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandling();
        mockBehandlingsresultat();

        eessiService.opprettOgSendSed(BEHANDLING_ID, List.of("SE:123"), BucType.LA_BUC_04, null, null);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(lagBehandlingsresultat()), eq(MedlemsperiodeType.LOVVALGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_04), eq(true));
    }

    @Test
    void opprettBucOgSed_verifiserKorrektSedType() throws Exception {
        OpprettSedDto opprettSedDto = new OpprettSedDto();
        opprettSedDto.setRinaUrl("localhost:3000");
        when(eessiConsumer.opprettBucOgSed(any(SedDataDto.class), any(), any(BucType.class), anyBoolean())).thenReturn(opprettSedDto);
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        mockBehandlingsresultat();

        eessiService.opprettBucOgSed(lagBehandling(), BucType.LA_BUC_01, List.of("SE:001"), Collections.emptyList());
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), anyCollection(), eq(BucType.LA_BUC_01), eq(false));
    }

    @Test
    void hentMottakerinstitusjoner_forventListeMedRettType() throws MelosysException {
        when(eessiConsumer.hentMottakerinstitusjoner(anyString(), anyList())).thenReturn(Arrays.asList(
            easyRandom.nextObject(Institusjon.class),
            easyRandom.nextObject(Institusjon.class)
        ));

        List<Institusjon> mottakerinstitusjoner = eessiService.hentEessiMottakerinstitusjoner("LA_BUC_01", List.of("FR"));

        verify(eessiConsumer).hentMottakerinstitusjoner(anyString(), anyList());
        assertThat(mottakerinstitusjoner).hasSize(2).hasOnlyElementsOfType(Institusjon.class);
    }

    @Test
    void hentTilknyttedeBucer_forventListeMedRettType() throws MelosysException {
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
    void hentTilknyttedeBucer_medFeilIConsumer_forventException() throws MelosysException {
        when(eessiConsumer.hentTilknyttedeBucer(anyLong(), anyList())).thenThrow(new IntegrasjonException("Error!"));
        assertThatExceptionOfType(MelosysException.class)
            .isThrownBy(() ->
                eessiService.hentTilknyttedeBucer(123L, Collections.singletonList("utkast")));
    }

    @Test
    void støtterAutomatiskBehandling_verifiserA001A003A009A010støtterAutomatiskBehandling() throws Exception {
        List<String> sedTyperAutomatiskBehandling = Arrays.asList(
            SedType.A001.name(),
            SedType.A009.name(),
            SedType.A010.name()
        );

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setLovvalgsland(Landkoder.DE.name());
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(eq("123"))).thenReturn(melosysEessiMelding);

        for (String sedType : sedTyperAutomatiskBehandling) {
            melosysEessiMelding.setSedType(sedType);
            assertThat(eessiService.støtterAutomatiskBehandling("123")).isTrue();
        }
    }

    @Test
    void støtterAutomatiskBehandling_verifiserStøtterIkkeAutomatiskBehandling() throws Exception {
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
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(eq("123"))).thenReturn(melosysEessiMelding);

        for (String sedType : sedTyperIkkeAutomatiskBehandling) {
            melosysEessiMelding.setSedType(sedType);
            assertThat(eessiService.støtterAutomatiskBehandling("123")).isFalse();
        }
    }

    @Test
    void støtterAutomatiskBehandling_nullVerdi_forventFalse() throws Exception {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType(null);
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(eq("123"))).thenReturn(melosysEessiMelding);
        assertThat(eessiService.støtterAutomatiskBehandling("123")).isFalse();
    }

    @Test
    void støtterAutomatiskBehandling_a003ikkeUtpekt_verifiserStøtterAutomatiskBehandling() throws Exception {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setLovvalgsland(Landkoder.SE.name());
        melosysEessiMelding.setSedType("A003");
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(eq("123"))).thenReturn(melosysEessiMelding);
        assertThat(eessiService.støtterAutomatiskBehandling("123")).isTrue();
    }

    @Test
    void støtterAutomatiskBehandling_a003erUtpekt_verifiserStøtterIkkeAutomatiskBehandling() throws Exception {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.name());
        melosysEessiMelding.setSedType("A003");
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(eq("123"))).thenReturn(melosysEessiMelding);
        assertThat(eessiService.støtterAutomatiskBehandling("123")).isTrue();
    }

    @Test
    void hentSakForRinaSaksnummer_forventOptionalIkkePresent() throws MelosysException {
        when(eessiConsumer.hentSakForRinasaksnummer(anyString()))
            .thenReturn(Collections.emptyList());
        Optional<Long> res = eessiService.finnSakForRinasaksnummer("123");
        assertThat(res).isNotPresent();
    }

    @Test
    void hentSakForRinaSaksnummer_forventOptionalPresent() throws MelosysException {
        when(eessiConsumer.hentSakForRinasaksnummer(anyString()))
            .thenReturn(Collections.singletonList(new SaksrelasjonDto(123L, "123", "123")));
        Optional<Long> res = eessiService.finnSakForRinasaksnummer("123");
        assertThat(res).isPresent();
    }

    @Test
    void lagreSaksrelasjon_validerInput() throws MelosysException {
        eessiService.lagreSaksrelasjon(123L, "123", "312");
        verify(eessiConsumer).lagreSaksrelasjon(any());
    }

    @Test
    void sendAnmodningUnntakSvar_forventKall() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(new SedDokument());
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        mockBehandlingsresultat();

        eessiService.sendAnmodningUnntakSvar(BEHANDLING_ID);

        verify(behandlingService).hentBehandling(eq(BEHANDLING_ID));
        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.ANMODNINGSPERIODE));
        verify(dokumentdataGrunnlagFactory).av(any());
        verify(eessiConsumer).sendSedPåEksisterendeBuc(any(SedDataDto.class), any(), eq(SedType.A002));
    }

    @Test
    void sendGodkjenningArbeidFlereLand() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(new SedDokument());
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        mockBehandlingsresultat();

        eessiService.sendGodkjenningArbeidFlereLand(BEHANDLING_ID, null);

        verify(behandlingService).hentBehandling(eq(BEHANDLING_ID));
        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.LOVVALGSPERIODE));
        verify(dokumentdataGrunnlagFactory).av(any());
        verify(eessiConsumer).sendSedPåEksisterendeBuc(any(SedDataDto.class), any(), eq(SedType.A012));
    }

    @Test
    void genererSedPdf_sedA001_medlemsperiodeTypeAnmodningsperiode() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        mockBehandlingsresultat();

        byte[] pdf = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A001);

        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.ANMODNINGSPERIODE));
        verify(eessiConsumer).genererSedPdf(any(), any());
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    void genererSedPdf_sedA003MedUtpekingsperiode_medlemsperiodeTypeUtpekingsperiode() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));

        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());
        when(behandlingsresultatService.hentBehandlingsresultat(eq(BEHANDLING_ID))).thenReturn(behandlingsresultat);

        byte[] pdf = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A003);

        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.UTPEKINGSPERIODE));
        verify(eessiConsumer).genererSedPdf(any(), any());
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    void genererSedPdf_sedA009_medlemsperiodeTypeLovvalgsperiode() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));

        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());
        when(behandlingsresultatService.hentBehandlingsresultat(eq(BEHANDLING_ID))).thenReturn(behandlingsresultat);

        byte[] pdf = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A009);

        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.LOVVALGSPERIODE));
        verify(eessiConsumer).genererSedPdf(any(), any());
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    void genererSedForhåndsvisning_medSedPdfData_verifiserSedDataDtoPreutfylt() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);
        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        mockBehandlingsresultat();
        mockBehandling();

        SedPdfData sedPdfData = new SedPdfData();
        sedPdfData.setVilSendeAnmodningOmMerInformasjon(null);
        sedPdfData.setNyttLovvalgsland("SE");
        byte[] pdf = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A001, sedPdfData);

        verify(behandlingService).hentBehandling(eq(BEHANDLING_ID));
        verify(dokumentdataGrunnlagFactory).av(any());
        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.ANMODNINGSPERIODE));
        verify(eessiConsumer).genererSedPdf(sedDataDtoCaptor.capture(), any());
        assertThat(pdf).isEqualTo(PDF);

        SedDataDto sedDataDto = sedDataDtoCaptor.getValue();
        assertThat(sedDataDto.getUtpekingAvvis()).isNotNull()
            .extracting(UtpekingAvvisDto::getNyttLovvalgsland)
            .isEqualTo(sedPdfData.getNyttLovvalgsland());
    }

    @Test
    void hentSedTypeForAnmodningUnntakSvar_forventA002() throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.hentValidertAnmodningsperiode()
            .getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        when(behandlingsresultatService.hentBehandlingsresultat(eq(BEHANDLING_ID))).thenReturn(behandlingsresultat);

        SedType sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(BEHANDLING_ID);

        assertThat(sedType).isEqualTo(SedType.A002);
    }

    @Test
    void hentSedTypeForAnmodningUnntakSvar_forventA011() throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.hentValidertAnmodningsperiode()
            .getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        when(behandlingsresultatService.hentBehandlingsresultat(eq(BEHANDLING_ID))).thenReturn(behandlingsresultat);

        SedType sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(BEHANDLING_ID);

        assertThat(sedType).isEqualTo(SedType.A011);
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toMottakereToMottakerLandMottakereKorrektSatt_returnererMottakerInstitusjoner() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final String mottakerBelgia = "BE:12222";
        final String mottakerTyskland = "DE:4444";
        final Set<String> valgteMottakerInstitusjoner = Set.of(mottakerBelgia, mottakerTyskland);

        final Institusjon institusjonBelgia1 = new Institusjon(mottakerBelgia, null, Landkoder.BE.getKode());
        final Institusjon institusjonBelgia2 = new Institusjon("BE:9999", null, Landkoder.BE.getKode());
        final Institusjon institusjonTyskland1 = new Institusjon(mottakerTyskland, null, Landkoder.DE.getKode());
        final Institusjon institusjonTyskland2 = new Institusjon("DE:9999", null, Landkoder.DE.getKode());


        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.BE.getKode())))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia2));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.DE.getKode())))
            .thenReturn(List.of(institusjonTyskland1, institusjonTyskland2));

        Set<String> avklarteMottakerInstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
        verify(eessiConsumer, times(2)).hentMottakerinstitusjoner(eq(bucType.name()), anyList());
        assertThat(avklarteMottakerInstitusjoner).isEqualTo(valgteMottakerInstitusjoner);
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toMottakereSisteErIkkeEessiReady_returnererTomListe() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final String mottakerBelgia = "BE:12222";
        final String mottakerTyskland = "DE:4444";
        final Set<String> valgteMottakerInstitusjoner = Set.of(mottakerBelgia, mottakerTyskland);

        final Institusjon institusjonBelgia1 = new Institusjon(mottakerBelgia, null, Landkoder.BE.getKode());
        final Institusjon institusjonBelgia2 = new Institusjon("BE:9999", null, Landkoder.BE.getKode());

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.BE.getKode())))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia2));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.DE.getKode())))
            .thenReturn(Collections.emptyList());

        Set<String> avklarteMottakerInstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
        verify(eessiConsumer, times(2)).hentMottakerinstitusjoner(eq(bucType.name()), anyList());
        assertThat(avklarteMottakerInstitusjoner).isEmpty();
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toLandInstitusjonManglerForSiste_kasterException() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final String mottakerBelgia = "BE:12222";
        final String mottakerTyskland = "DE:4444";
        final Set<String> valgteMottakerInstitusjoner = Set.of(mottakerBelgia);

        final Institusjon institusjonBelgia1 = new Institusjon(mottakerBelgia, null, Landkoder.BE.getKode());
        final Institusjon institusjonBelgia2 = new Institusjon("BE:9999", null, Landkoder.BE.getKode());
        final Institusjon institusjonTyskland1 = new Institusjon(mottakerTyskland, null, Landkoder.DE.getKode());
        final Institusjon institusjonTyskland2 = new Institusjon("DE:9999", null, Landkoder.DE.getKode());


        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.BE.getKode())))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia2));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.DE.getKode())))
            .thenReturn(List.of(institusjonTyskland1, institusjonTyskland2));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() ->
                eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType))
            .withMessageContaining("Finner ingen gyldig mottakerinstitusjon for arbeidsland Tyskland");
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toLandInstitusjonManglerForSiste2_kasterException() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final String mottakerBelgia = "BE:12222";
        final String mottakerBelgia2 = "BE:123131";
        final String mottakerTyskland = "DE:4444";
        final Set<String> valgteMottakerInstitusjoner = Set.of(mottakerBelgia, mottakerBelgia2, mottakerTyskland);

        final Institusjon institusjonBelgia1 = new Institusjon(mottakerBelgia, null, Landkoder.BE.getKode());
        final Institusjon institusjonBelgia2 = new Institusjon(mottakerBelgia2, null, Landkoder.BE.getKode());
        final Institusjon institusjonBelgia3 = new Institusjon("BE:9999", null, Landkoder.BE.getKode());
        final Institusjon institusjonTyskland1 = new Institusjon(mottakerTyskland, null, Landkoder.DE.getKode());
        final Institusjon institusjonTyskland2 = new Institusjon("DE:9999", null, Landkoder.DE.getKode());

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.BE.getKode())))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia2, institusjonBelgia3));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.DE.getKode())))
            .thenReturn(List.of(institusjonTyskland1, institusjonTyskland2));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType))
            .withMessageContaining("Kan kun velge en mottakerinstitusjon per land. Validerte mottakere:");
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toLandErPåkobletIngenInstitusjonValgt_kasterException() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final Set<String> valgteMottakerInstitusjoner = Collections.emptySet();

        final Institusjon institusjonBelgia = new Institusjon("BE:9999", null, Landkoder.BE.getKode());
        final Institusjon institusjonTyskland = new Institusjon("DE:9999", null, Landkoder.DE.getKode());

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.BE.getKode())))
            .thenReturn(List.of(institusjonBelgia));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.DE.getKode())))
            .thenReturn(List.of(institusjonTyskland));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() ->
                eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType))
            .withMessageContaining(
                "Finner ingen gyldig mottakerinstitusjon for arbeidsland " + Landkoder.BE.getBeskrivelse() + System.lineSeparator() +
                    "Finner ingen gyldig mottakerinstitusjon for arbeidsland " + Landkoder.DE.getBeskrivelse());
    }

    @Test
    void validerOgAvklarMottakerInstitusjonerForBuc_toLandEnErIkkePåkobletIngenInstitusjonValgt_returnererTomListe() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final Set<String> valgteMottakerInstitusjoner = Collections.emptySet();

        final Institusjon institusjonBelgia = new Institusjon("BE:44444", null, Landkoder.BE.getKode());

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.BE.getKode())))
            .thenReturn(List.of(institusjonBelgia));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), List.of(Landkoder.DE.getKode())))
            .thenReturn(Collections.emptyList());

        Set<String> avklarteMottakere = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
        assertThat(avklarteMottakere).isEmpty();
    }

    @Test
    void landErEessiReady_toLandEtErEessiReady_forventFalse() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_01;
        final List<Landkoder> land = List.of(Landkoder.SE, Landkoder.DK);

        when(eessiConsumer.hentMottakerinstitusjoner(eq(bucType.name()), eq(List.of(Landkoder.SE.getKode()))))
            .thenReturn(List.of(new Institusjon("2", "", "")));

        assertThat(eessiService.landErEessiReady(bucType.name(), land)).isFalse();
    }

    @Test
    void landErEessiReady_toLandAlleErEessiReady_forventTrue() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_01;
        final List<Landkoder> land = List.of(Landkoder.SE, Landkoder.DK);

        when(eessiConsumer.hentMottakerinstitusjoner(eq(bucType.name()), any()))
            .thenReturn(List.of(new Institusjon("2", "", "")));

        assertThat(eessiService.landErEessiReady(bucType.name(), land)).isTrue();
    }
}