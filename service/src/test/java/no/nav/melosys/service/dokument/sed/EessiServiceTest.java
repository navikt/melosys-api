package no.nav.melosys.service.dokument.sed;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
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
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.integrasjon.eessi.dto.SedGrunnlagDto;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EessiServiceTest {
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

    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EasyRandom easyRandom = new EasyRandom();

    @Before
    public void setup() throws Exception {
        eessiService = new EessiService(sedDataBygger, dokumentdataGrunnlagFactory,
            eessiConsumer, behandlingService, behandlingsresultatService);

        behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("123");
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        behandlingsresultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.SK);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));

        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true))).thenReturn(new OpprettSedDto());
    }

    @Test
    public void opprettOgSendSed_buc03_ingenMedlemsperiodeType() throws Exception {
        eessiService.opprettOgSendSed(behandling.getId(), List.of("SE:123"), BucType.LA_BUC_03);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(behandlingsresultat), eq(MedlemsperiodeType.INGEN));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_03), eq(true));
    }

    @Test
    public void opprettOgSendSed_buc01_medlemsperiodeTypeAnmodningsperiode() throws Exception {
        eessiService.opprettOgSendSed(behandling.getId(), List.of("SE:123"), BucType.LA_BUC_01);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(behandlingsresultat), eq(MedlemsperiodeType.ANMODNINGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_01), eq(true));
    }

    @Test
    public void opprettOgSendSed_buc02IngenUtpekingsperiode_medlemsperiodeTypeLovvalgsperiode() throws Exception {
        eessiService.opprettOgSendSed(behandling.getId(), List.of("SE:123"), BucType.LA_BUC_02);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(behandlingsresultat), eq(MedlemsperiodeType.LOVVALGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_02), eq(true));
    }

    @Test
    public void opprettOgSendSed_buc02MedUtpekingsperiode_medlemsperiodeTypeUtpekingsperiode() throws Exception {
        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());
        eessiService.opprettOgSendSed(behandling.getId(), List.of("SE:123"), BucType.LA_BUC_02);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(behandlingsresultat), eq(MedlemsperiodeType.UTPEKINGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_02), eq(true));
    }

    @Test
    public void opprettOgSendSed_buc04_medlemsperiodeTypeLovvalgsperiode() throws Exception {
        eessiService.opprettOgSendSed(behandling.getId(), List.of("SE:123"), BucType.LA_BUC_04);
        verify(sedDataBygger).lag(any(SedDataGrunnlag.class), eq(behandlingsresultat), eq(MedlemsperiodeType.LOVVALGSPERIODE));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_04), eq(true));
    }

    @Test
    public void opprettBucOgSed_verifiserKorrektSedType() throws Exception {
        OpprettSedDto opprettSedDto = new OpprettSedDto();
        opprettSedDto.setRinaUrl("localhost:3000");
        when(eessiConsumer.opprettBucOgSed(any(SedDataDto.class), any(), any(BucType.class), anyBoolean())).thenReturn(opprettSedDto);

        eessiService.opprettBucOgSed(behandling, BucType.LA_BUC_01, List.of("SE:001"));
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_01), eq(false));
    }

    @Test
    public void hentMottakerinstitusjoner_forventListeMedRettType() throws MelosysException {
        when(eessiConsumer.hentMottakerinstitusjoner(anyString(), anyString())).thenReturn(Arrays.asList(
            easyRandom.nextObject(Institusjon.class),
            easyRandom.nextObject(Institusjon.class)
        ));

        List<Institusjon> mottakerinstitusjoner = eessiService.hentEessiMottakerinstitusjoner("LA_BUC_01", "FR");

        verify(eessiConsumer).hentMottakerinstitusjoner(anyString(), anyString());
        assertThat(mottakerinstitusjoner).hasSize(2);
        assertThat(mottakerinstitusjoner).hasOnlyElementsOfType(Institusjon.class);
    }

    @Test
    public void hentTilknyttedeBucer_forventListeMedRettType() throws MelosysException {
        when(eessiConsumer.hentTilknyttedeBucer(anyLong(), anyList())).thenReturn(Arrays.asList(
            easyRandom.nextObject(BucInformasjon.class),
            easyRandom.nextObject(BucInformasjon.class),
            easyRandom.nextObject(BucInformasjon.class)
        ));

        List<BucInformasjon> tilknyttedeBucer = eessiService.hentTilknyttedeBucer(123L, Arrays.asList("utkast", "sendt"));

        verify(eessiConsumer).hentTilknyttedeBucer(anyLong(), anyList());
        assertThat(tilknyttedeBucer).hasSize(3);
        assertThat(tilknyttedeBucer).hasOnlyElementsOfType(BucInformasjon.class);
    }

    @Test(expected = MelosysException.class)
    public void hentTilknyttedeBucer_medFeilIConsumer_forventException() throws MelosysException {
        when(eessiConsumer.hentTilknyttedeBucer(anyLong(), anyList())).thenThrow(new IntegrasjonException("Error!"));
        eessiService.hentTilknyttedeBucer(123L, Collections.singletonList("utkast"));
    }

    @Test
    public void støtterAutomatiskBehandling_verifiserA001A003A009A010støtterAutomatiskBehandling() throws Exception {
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
    public void støtterAutomatiskBehandling_verifiserStøtterIkkeAutomatiskBehandling() throws Exception {
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
    public void støtterAutomatiskBehandling_nullVerdi_forventFalse() throws Exception {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType(null);
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(eq("123"))).thenReturn(melosysEessiMelding);
        assertThat(eessiService.støtterAutomatiskBehandling("123")).isFalse();
    }

    @Test
    public void støtterAutomatiskBehandling_a003ikkeUtpekt_verifiserStøtterAutomatiskBehandling() throws Exception {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setLovvalgsland(Landkoder.SE.name());
        melosysEessiMelding.setSedType("A003");
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(eq("123"))).thenReturn(melosysEessiMelding);
        assertThat(eessiService.støtterAutomatiskBehandling("123")).isTrue();
    }

    @Test
    public void støtterAutomatiskBehandling_a003erUtpekt_verifiserStøtterIkkeAutomatiskBehandling() throws Exception {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.name());
        melosysEessiMelding.setSedType("A003");
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(eq("123"))).thenReturn(melosysEessiMelding);
        assertThat(eessiService.støtterAutomatiskBehandling("123")).isTrue();
    }

    @Test
    public void hentSakForRinaSaksnummer_forventOptionalIkkePresent() throws MelosysException {
        when(eessiConsumer.hentSakForRinasaksnummer(anyString()))
            .thenReturn(Collections.emptyList());
        Optional<Long> res = eessiService.finnSakForRinasaksnummer("123");
        assertThat(res).isNotPresent();
    }

    @Test
    public void hentSakForRinaSaksnummer_forventOptionalPresent() throws MelosysException {
        when(eessiConsumer.hentSakForRinasaksnummer(anyString()))
            .thenReturn(Collections.singletonList(new SaksrelasjonDto(123L, "123", "123")));
        Optional<Long> res = eessiService.finnSakForRinasaksnummer("123");
        assertThat(res).isPresent();
    }

    @Test
    public void lagreSaksrelasjon_validerInput() throws MelosysException {
        eessiService.lagreSaksrelasjon(123L, "123", "312");
        verify(eessiConsumer).lagreSaksrelasjon(any());
    }

    @Test
    public void sendAnmodningUnntakSvar_forventKall() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(new SedDokument());
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        eessiService.sendAnmodningUnntakSvar(1L);

        verify(behandlingService).hentBehandling(eq(1L));
        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.ANMODNINGSPERIODE));
        verify(dokumentdataGrunnlagFactory).av(any());
        verify(eessiConsumer).sendSedPåEksisterendeBuc(any(SedDataDto.class), any(), eq(SedType.A002));
    }

    @Test
    public void sendGodkjenningArbeidFlereLand() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(new SedDokument());
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        eessiService.sendGodkjenningArbeidFlereLand(1L);

        verify(behandlingService).hentBehandling(eq(1L));
        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.LOVVALGSPERIODE));
        verify(dokumentdataGrunnlagFactory).av(any());
        verify(eessiConsumer).sendSedPåEksisterendeBuc(any(SedDataDto.class), any(), eq(SedType.A012));
    }

    @Test
    public void genererSedPdf_sedA001_medlemsperiodeTypeAnmodningsperiode() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);

        byte[] pdf = eessiService.genererSedPdf(1L, SedType.A001);

        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.ANMODNINGSPERIODE));
        verify(eessiConsumer).genererSedPdf(any(), any());
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    public void genererSedPdf_sedA003MedUtpekingsperiode_medlemsperiodeTypeUtpekingsperiode() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);

        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());

        byte[] pdf = eessiService.genererSedPdf(1L, SedType.A003);

        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.UTPEKINGSPERIODE));
        verify(eessiConsumer).genererSedPdf(any(), any());
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    public void genererSedPdf_sedA009_medlemsperiodeTypeLovvalgsperiode() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedPdf(any(), any())).thenReturn(PDF);

        behandlingsresultat.getUtpekingsperioder().add(new Utpekingsperiode());

        byte[] pdf = eessiService.genererSedPdf(1L, SedType.A009);

        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.LOVVALGSPERIODE));
        verify(eessiConsumer).genererSedPdf(any(), any());
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    public void hentSedTypeForAnmodningUnntakSvar_forventA002() throws IkkeFunnetException {
        behandlingsresultat.hentValidertAnmodningsperiode()
            .getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        SedType sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(1L);

        assertThat(sedType).isEqualTo(SedType.A002);
    }

    @Test
    public void hentSedTypeForAnmodningUnntakSvar_forventA011() throws IkkeFunnetException {
        behandlingsresultat.hentValidertAnmodningsperiode()
            .getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        SedType sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(1L);

        assertThat(sedType).isEqualTo(SedType.A011);
    }

    @Test
    public void validerOgAvklarMottakerInstitusjonerForBuc_toMottakereToMottakerLandMottakereKorrektSatt_returnererMottakerInstitusjoner() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final String mottakerBelgia = "BE:12222";
        final String mottakerTyskland = "DE:4444";
        final List<String> valgteMottakerInstitusjoner = List.of(mottakerBelgia, mottakerTyskland);

        final Institusjon institusjonBelgia1 = new Institusjon(mottakerBelgia, null, Landkoder.BE.getKode());
        final Institusjon institusjonBelgia2 = new Institusjon("BE:9999", null, Landkoder.BE.getKode());
        final Institusjon institusjonTyskland1 = new Institusjon(mottakerTyskland, null, Landkoder.DE.getKode());
        final Institusjon institusjonTyskland2 = new Institusjon("DE:9999", null, Landkoder.DE.getKode());


        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.BE.getKode()))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia2));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.DE.getKode()))
            .thenReturn(List.of(institusjonTyskland1, institusjonTyskland2));

        List<String> avklarteMottakerInstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
        verify(eessiConsumer, times(2)).hentMottakerinstitusjoner(eq(bucType.name()), anyString());
        assertThat(avklarteMottakerInstitusjoner).isEqualTo(valgteMottakerInstitusjoner);
    }

    @Test
    public void validerOgAvklarMottakerInstitusjonerForBuc_toMottakereSisteErIkkeEessiReady_returnererTomListe() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final String mottakerBelgia = "BE:12222";
        final String mottakerTyskland = "DE:4444";
        final List<String> valgteMottakerInstitusjoner = List.of(mottakerBelgia, mottakerTyskland);

        final Institusjon institusjonBelgia1 = new Institusjon(mottakerBelgia, null, Landkoder.BE.getKode());
        final Institusjon institusjonBelgia2 = new Institusjon("BE:9999", null, Landkoder.BE.getKode());

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.BE.getKode()))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia2));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.DE.getKode()))
            .thenReturn(Collections.emptyList());

        List<String> avklarteMottakerInstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
        verify(eessiConsumer, times(2)).hentMottakerinstitusjoner(eq(bucType.name()), anyString());
        assertThat(avklarteMottakerInstitusjoner).isEmpty();
    }

    @Test
    public void validerOgAvklarMottakerInstitusjonerForBuc_toLandInstitusjonManglerForSiste_kasterException() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final String mottakerBelgia = "BE:12222";
        final String mottakerTyskland = "DE:4444";
        final List<String> valgteMottakerInstitusjoner = List.of(mottakerBelgia);

        final Institusjon institusjonBelgia1 = new Institusjon(mottakerBelgia, null, Landkoder.BE.getKode());
        final Institusjon institusjonBelgia2 = new Institusjon("BE:9999", null, Landkoder.BE.getKode());
        final Institusjon institusjonTyskland1 = new Institusjon(mottakerTyskland, null, Landkoder.DE.getKode());
        final Institusjon institusjonTyskland2 = new Institusjon("DE:9999", null, Landkoder.DE.getKode());


        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.BE.getKode()))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia2));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.DE.getKode()))
            .thenReturn(List.of(institusjonTyskland1, institusjonTyskland2));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Finner ingen gyldig mottakerinstitusjon for arbeidsland Tyskland");

        eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
    }

    @Test
    public void validerOgAvklarMottakerInstitusjonerForBuc_toLandInstitusjonManglerForSiste2_kasterException() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final String mottakerBelgia = "BE:12222";
        final String mottakerBelgia2 = "BE:123131";
        final String mottakerTyskland = "DE:4444";
        final List<String> valgteMottakerInstitusjoner = List.of(mottakerBelgia, mottakerBelgia2, mottakerTyskland);

        final Institusjon institusjonBelgia1 = new Institusjon(mottakerBelgia, null, Landkoder.BE.getKode());
        final Institusjon institusjonBelgia2 = new Institusjon(mottakerBelgia2, null, Landkoder.BE.getKode());
        final Institusjon institusjonBelgia3 = new Institusjon("BE:9999", null, Landkoder.BE.getKode());
        final Institusjon institusjonTyskland1 = new Institusjon(mottakerTyskland, null, Landkoder.DE.getKode());
        final Institusjon institusjonTyskland2 = new Institusjon("DE:9999", null, Landkoder.DE.getKode());

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.BE.getKode()))
            .thenReturn(List.of(institusjonBelgia1, institusjonBelgia2, institusjonBelgia3));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.DE.getKode()))
            .thenReturn(List.of(institusjonTyskland1, institusjonTyskland2));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Kan kun velge en mottakerinstitusjon per land. Validerte mottakere:");

        eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
    }

    @Test
    public void validerOgAvklarMottakerInstitusjonerForBuc_toLandErPåkobletIngenInstitusjonValgt_kasterException() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final List<String> valgteMottakerInstitusjoner = Collections.emptyList();

        final Institusjon institusjonBelgia = new Institusjon("BE:9999", null, Landkoder.BE.getKode());
        final Institusjon institusjonTyskland = new Institusjon("DE:9999", null, Landkoder.DE.getKode());

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.BE.getKode()))
            .thenReturn(List.of(institusjonBelgia));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.DE.getKode()))
            .thenReturn(List.of(institusjonTyskland));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage(
            "Finner ingen gyldig mottakerinstitusjon for arbeidsland " + Landkoder.BE.getBeskrivelse() + System.lineSeparator() +
                "Finner ingen gyldig mottakerinstitusjon for arbeidsland " + Landkoder.DE.getBeskrivelse());

        eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
    }

    @Test
    public void validerOgAvklarMottakerInstitusjonerForBuc_toLandEnErIkkePåkobletIngenInstitusjonValgt_returnererTomListe() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_02;
        final List<Landkoder> mottakerLand = List.of(Landkoder.BE, Landkoder.DE);

        final List<String> valgteMottakerInstitusjoner = Collections.emptyList();

        final Institusjon institusjonBelgia = new Institusjon("BE:44444", null, Landkoder.BE.getKode());

        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.BE.getKode()))
            .thenReturn(List.of(institusjonBelgia));
        when(eessiConsumer.hentMottakerinstitusjoner(bucType.name(), Landkoder.DE.getKode()))
            .thenReturn(Collections.emptyList());

        List<String> avklarteMottakere = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType);
        assertThat(avklarteMottakere).isEmpty();
    }

    @Test
    public void landErEessiReady_toLandEtErEessiReady_forventFalse() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_01;
        final List<Landkoder> land = List.of(Landkoder.SE, Landkoder.DK);

        when(eessiConsumer.hentMottakerinstitusjoner(eq(bucType.name()), eq(Landkoder.SE.getKode())))
            .thenReturn(List.of(new Institusjon("2", "", "")));

        assertThat(eessiService.landErEessiReady(bucType.name(), land)).isFalse();
    }

    @Test
    public void landErEessiReady_toLandAlleErEessiReady_forventTrue() throws MelosysException {
        final BucType bucType = BucType.LA_BUC_01;
        final List<Landkoder> land = List.of(Landkoder.SE, Landkoder.DK);

        when(eessiConsumer.hentMottakerinstitusjoner(eq(bucType.name()), any()))
            .thenReturn(List.of(new Institusjon("2", "", "")));

        assertThat(eessiService.landErEessiReady(bucType.name(), land)).isTrue();
    }

    @Test
    public void hentSedGrunnlag() throws MelosysException {
        when(eessiConsumer.hentSedGrunnlag(anyString(), anyString())).thenReturn(new EasyRandom().nextObject(SedGrunnlagDto.class));

        SedGrunnlag sedGrunnlag = eessiService.hentSedGrunnlag("123", "abc");

        assertThat(sedGrunnlag).isNotNull();
        assertThat(sedGrunnlag).isInstanceOf(SedGrunnlag.class);
    }
}