package no.nav.melosys.service.utpeking;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UtpekingServiceTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private UtpekingsperiodeRepository utpekingsperiodeRepository;
    @Mock
    private LandvelgerService landvelgerService;

    private UtpekingService utpekingService;

    private final long behandlingID = 431;
    private Behandling behandling = new Behandling();
    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private Fagsak fagsak = new Fagsak();


    @Before
    public void setup() throws FunksjonellException {
        utpekingService = new UtpekingService(behandlingService, behandlingsresultatService, eessiService, oppgaveService, prosessinstansService, utpekingsperiodeRepository, landvelgerService);

        fagsak.setBehandlinger(List.of(behandling));
        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(behandlingID))).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingID))).thenReturn(behandlingsresultat);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(eq(behandlingID))).thenReturn(List.of(Landkoder.SE));
    }

    @Test
    public void utpekLovvalgsland_harUtpekingsperiode_prosessinstansBlirOpprettet() throws MelosysException {
        Utpekingsperiode utpekingsperiode = new Utpekingsperiode(LocalDate.now(), LocalDate.now(), Landkoder.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, null);
        behandlingsresultat.getUtpekingsperioder().add(utpekingsperiode);

        final Set<String> mottakerInstitusjoner = Set.of("SE:123");
        when(eessiService.validerOgAvklarMottakerInstitusjonerForBuc(eq(mottakerInstitusjoner), eq(List.of(Landkoder.SE)), eq(BucType.LA_BUC_02)))
            .thenReturn(mottakerInstitusjoner);

        utpekingService.utpekLovvalgsland(fagsak, mottakerInstitusjoner, null, null);

        verify(prosessinstansService).opprettProsessinstansUtpekAnnetLand(eq(behandling), eq(Landkoder.SE), eq(mottakerInstitusjoner), isNull(), isNull());
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq(fagsak.getSaksnummer()));
    }

    @Test
    public void avvisUtpeking_utpekingAvAnnetLand_oppdaterUtfallRegistreringUnntak() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);

        utpekingService.avvisUtpeking(behandlingID, lagUtpekingAvvis());

        verify(behandlingsresultatService).oppdaterUtfallRegistreringUnntak(eq(behandlingID), eq(Utfallregistreringunntak.IKKE_GODKJENT));
        verify(prosessinstansService).opprettProsessinstansAvvisUtpeking(eq(behandling), any(UtpekingAvvis.class));
    }

    @Test
    public void avvisUtpeking_utpekingAvNorge_oppdaterUtfallUtpeking() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);

        utpekingService.avvisUtpeking(behandlingID, lagUtpekingAvvis());

        verify(behandlingsresultatService).oppdaterUtfallUtpeking(eq(behandlingID), eq(Utfallregistreringunntak.IKKE_GODKJENT));
        verify(prosessinstansService).opprettProsessinstansAvvisUtpeking(eq(behandling), any(UtpekingAvvis.class));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq(fagsak.getSaksnummer()));
    }

    @Test
    public void avvisUtpeking_utsendtArbeidtaker_ikkeStøttetKasterException() {
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> utpekingService.avvisUtpeking(behandlingID, lagUtpekingAvvis()))
            .withMessageContaining("Kan ikke avvise utpeking for en behandling med tema");
    }

    @Test
    public void avvisUtpeking_utenBegrunnelse_begrunnelsePåkrevdKasterException() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> utpekingService.avvisUtpeking(behandlingID, new UtpekingAvvis()))
            .withMessageContaining("Du må oppgi en begrunnelse for å kunne avslå en utpeking");
    }

    @Test
    public void avvisUtpeking_utenEtterspørInformasjon_etterspørInfoPåkrevdKasterException() {
        UtpekingAvvis utpekingAvvis = new UtpekingAvvis();
        utpekingAvvis.setBegrunnelse("fordi og derfor");

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> utpekingService.avvisUtpeking(behandlingID, utpekingAvvis))
            .withMessageContaining("Du må oppgi om forespørsel om mer informasjon vil bli sendt");
    }

    @Test
    public void avvisUtpeking_behandlingInaktiv_kasterException() {
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> utpekingService.avvisUtpeking(behandlingID, lagUtpekingAvvis()))
            .withMessageContaining("er ikke aktiv");
    }

    @Test
    public void oppdaterSendtUtland_ikkeSattFraFør_oppdateres() throws FunksjonellException, TekniskException {
        Utpekingsperiode utpekingsperiode = new Utpekingsperiode();
        utpekingsperiode.setId(1L);

        utpekingService.oppdaterSendtUtland(utpekingsperiode);
        verify(utpekingsperiodeRepository).save(utpekingsperiode);
        assertThat(utpekingsperiode.getSendtUtland()).isNotNull();
    }

    @Test
    public void oppdaterSendtUtland_ikkePersistert_kasterException() {
        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> utpekingService.oppdaterSendtUtland(new Utpekingsperiode()))
            .withMessageContaining("Forsøk på å oppdatere en ikke-persistert utpekingsperiode");
    }

    @Test
    public void oppdaterSendtUtland_alleredeSendtUtland_kasterException() {
        Utpekingsperiode utpekingsperiode = new Utpekingsperiode();
        utpekingsperiode.setId(1L);
        utpekingsperiode.setSendtUtland(LocalDate.now());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> utpekingService.oppdaterSendtUtland(utpekingsperiode))
            .withMessageContaining("er allerede markert som sendtUtland");
    }

    private UtpekingAvvis lagUtpekingAvvis() {
        UtpekingAvvis utpekingAvvis = new UtpekingAvvis();
        utpekingAvvis.setBegrunnelse("taddaaa");
        utpekingAvvis.setEtterspørInformasjon(Boolean.TRUE);
        return utpekingAvvis;
    }
}