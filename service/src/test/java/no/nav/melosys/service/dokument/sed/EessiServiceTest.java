package no.nav.melosys.service.dokument.sed;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EasyRandom easyRandom = new EasyRandom();

    @Before
    public void setup() throws Exception {
        eessiService = new EessiService("true", sedDataBygger, dokumentdataGrunnlagFactory,
            eessiConsumer, behandlingService, behandlingsresultatService);

        behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("123");
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

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
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        when(dokumentdataGrunnlagFactory.av(any())).thenReturn(Mockito.mock(SedDataGrunnlagMedSoknad.class));

        when(sedDataBygger.lag(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
        when(sedDataBygger.lagUtkast(any(SedDataGrunnlag.class), any(Behandlingsresultat.class), any(MedlemsperiodeType.class))).thenReturn(new SedDataDto());
    }

    @Test
    public void opprettOgSendSed_verifiserKorrektSedType() throws Exception {
        when(eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true))).thenReturn(new OpprettSedDto());
        eessiService.opprettOgSendSed(behandling.getId(), BucType.LA_BUC_03);
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), any(), eq(BucType.LA_BUC_03), eq(true));
    }

    @Test
    public void opprettBucOgSed_verifiserKorrektSedType() throws Exception {
        OpprettSedDto opprettSedDto = new OpprettSedDto();
        opprettSedDto.setRinaUrl("localhost:3000");
        when(eessiConsumer.opprettBucOgSed(any(SedDataDto.class), any(), any(BucType.class), anyBoolean())).thenReturn(opprettSedDto);

        eessiService.opprettBucOgSed(behandling, BucType.LA_BUC_01, "SE", "SE:001");
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class),any(), eq(BucType.LA_BUC_01), eq(false));
    }

    @Test
    public void hentMottakerinstitusjoner_forventListeMedRettType() throws MelosysException {
        when(eessiConsumer.hentMottakerinstitusjoner(anyString())).thenReturn(Arrays.asList(
            easyRandom.nextObject(Institusjon.class),
            easyRandom.nextObject(Institusjon.class)
        ));

        List<Institusjon> mottakerinstitusjoner = eessiService.hentEessiMottakerinstitusjoner("LA_BUC_01");

        verify(eessiConsumer).hentMottakerinstitusjoner(anyString());
        assertThat(mottakerinstitusjoner).hasSize(2);
        assertThat(mottakerinstitusjoner).hasOnlyElementsOfType(Institusjon.class);
    }

    @Test(expected = MelosysException.class)
    public void hentMottakerinstitusjoner_medFeilIConsumer_forventTomListe() throws MelosysException {
        when(eessiConsumer.hentMottakerinstitusjoner(anyString())).thenThrow(new IntegrasjonException("Error!"));
        eessiService.hentEessiMottakerinstitusjoner("LA_BUC_01");
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

        for (String sedType : sedTyperAutomatiskBehandling) {
            assertThat(eessiService.støtterAutomatiskBehandling("123", sedType)).isTrue();
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

        for (String sedType : sedTyperIkkeAutomatiskBehandling) {
            assertThat(eessiService.støtterAutomatiskBehandling("123", sedType)).isFalse();
        }
    }

    @Test
    public void støtterAutomatiskBehandling_a003ikkeUtpekt_verifiserStøtterAutomatiskBehandling() throws Exception {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setLovvalgsland(Landkoder.SE.name());
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(eq("123"))).thenReturn(melosysEessiMelding);
        assertThat(eessiService.støtterAutomatiskBehandling("123", "A003")).isTrue();
    }

    @Test
    public void støtterAutomatiskBehandling_a003erUtpekt_verifiserStøtterIkkeAutomatiskBehandling() throws Exception {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.name());
        when(eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(eq("123"))).thenReturn(melosysEessiMelding);
        assertThat(eessiService.støtterAutomatiskBehandling("123", "A003")).isFalse();
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
    public void genererSedForhåndsvisning_forventPdf() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        when(eessiConsumer.genererSedForhåndsvisning(any(), any())).thenReturn(PDF);

        byte[] pdf = eessiService.genererSedForhåndsvisning(1L, SedType.A001);

        verify(behandlingService).hentBehandling(eq(1L));
        verify(dokumentdataGrunnlagFactory).av(any());
        verify(sedDataBygger).lagUtkast(any(SedDataGrunnlag.class), any(), eq(MedlemsperiodeType.ANMODNINGSPERIODE));
        verify(eessiConsumer).genererSedForhåndsvisning(any(), any());
        assertThat(pdf).isEqualTo(PDF);
    }
}