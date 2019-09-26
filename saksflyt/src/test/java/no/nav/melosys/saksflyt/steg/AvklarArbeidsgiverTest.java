package no.nav.melosys.saksflyt.steg;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AktoerRepository;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.steg.iv.AvklarArbeidsgiver;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklarArbeidsgiverTest {
    @Mock
    AktoerService aktoerService;
    @Mock
    BehandlingRepository behandlingRepository;
    @Mock
    BehandlingsresultatService behandlingsresultatService;
    @Mock
    Behandling behandling;
    @Mock
    AvklarteVirksomheterSystemService avklarteVirksomheterService;

    private AvklarArbeidsgiver steg;
    private Prosessinstans p;
    private AvklartVirksomhet avklartVirksomhet;
    private Fagsak fagsak;
    private Behandlingsresultat behandlingsresultat;
    private Lovvalgsperiode lovvalgsperiode;

    @Before
    public void setUp() throws IkkeFunnetException {
        aktoerService = mock(AktoerService.class);
        steg = new AvklarArbeidsgiver(aktoerService, avklarteVirksomheterService, behandlingRepository, behandlingsresultatService);

        p = new Prosessinstans();
        p.setBehandling(behandling);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnr");
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(behandling);
        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        avklartVirksomhet =
            new AvklartVirksomhet("Test", "123456789", null, Yrkesaktivitetstyper.LOENNET_ARBEID);
    }

    @Test
    public void utfør_medAvklartNorskVirksomhet_arbeidsgiveraktørOpprettes() throws FunksjonellException, TekniskException {
        AktoerRepository aktoerRepository = mock(AktoerRepository.class);
        AvklarArbeidsgiver steg = new AvklarArbeidsgiver(new AktoerService(aktoerRepository), avklarteVirksomheterService, behandlingRepository, behandlingsresultatService);

        List<AvklartVirksomhet> avklarteVirksomheter = Collections.singletonList(avklartVirksomhet);
        when(avklarteVirksomheterService.hentNorskeArbeidsgivere(any(), any())).thenReturn(avklarteVirksomheter);

        steg.utfør(p);

        verify(aktoerRepository).deleteAllByFagsakAndRolle(eq(fagsak), eq(Aktoersroller.ARBEIDSGIVER));

        Aktoer aktoer = new Aktoer();
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(Aktoersroller.ARBEIDSGIVER);
        aktoer.setOrgnr("123456789");
        verify(aktoerRepository).save(eq(aktoer));
    }

    @Test
    public void utfør_utenAvklartNorskVirksomhet_arbeidsgiveraktorerSlettes() throws FunksjonellException, TekniskException {
        AktoerRepository aktoerRepository = mock(AktoerRepository.class);
        AvklarArbeidsgiver steg = new AvklarArbeidsgiver(new AktoerService(aktoerRepository), avklarteVirksomheterService, behandlingRepository, behandlingsresultatService);

        steg.utfør(p);
        verify(aktoerRepository).deleteAllByFagsakAndRolle(eq(fagsak), eq(Aktoersroller.ARBEIDSGIVER));
        verify(aktoerRepository, never()).save(any());
    }

    @Test
    public void utfør_iverksettVedtakType_forventStegIvOppdaterMedl() throws Exception {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivere(any(), any())).thenReturn(Collections.singletonList(avklartVirksomhet));
        steg.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.IV_OPPDATER_MEDL);
    }

    @Test
    public void utfør_avslagManglendeOpplysningerTypeHarMedlperiodeID_forventStegIvOppdaterMedl() throws Exception {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivere(any(), any())).thenReturn(Collections.singletonList(avklartVirksomhet));
        behandlingsresultat.setType(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        lovvalgsperiode.setMedlPeriodeID(123L);
        steg.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.IV_OPPDATER_MEDL);
    }

    @Test
    public void utfør_avslagManglendeOpplysningerType_forventStegIvSendBrev() throws Exception {
        when(avklarteVirksomheterService.hentNorskeArbeidsgivere(any(), any())).thenReturn(Collections.singletonList(avklartVirksomhet));
        behandlingsresultat.setType(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        steg.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.IV_SEND_BREV);
    }

    @Test
    public void utfør_iverksettVedtakArt12_arbeidsgiverAktoererSkalOpprettes() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        steg.utfør(p);
        verify(aktoerService).erstattEksisterendeArbeidsgiveraktører(any(), any());
    }

    @Test
    public void utfør_iverksettVedtakArt13_arbeidsgiverAktoererSkalIkkeOpprettes() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        steg.utfør(p);
        verify(aktoerService, never()).erstattEksisterendeArbeidsgiveraktører(any(), any());
    }
}