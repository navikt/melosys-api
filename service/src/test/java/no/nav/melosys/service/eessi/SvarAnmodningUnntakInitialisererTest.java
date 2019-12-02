package no.nav.melosys.service.eessi;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.OppgaveOppdatering;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class SvarAnmodningUnntakInitialisererTest {

    private SvarAnmodningUnntakInitialiserer svarAnmodningUnntakInitialiserer;

    @Mock
    private FagsakService fagsakService;
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private GsakFasade gsakFasade;

    private final String AKTØR_ID = "223345325342";

    @Before
    public void setUp() {
        svarAnmodningUnntakInitialiserer = new SvarAnmodningUnntakInitialiserer(fagsakService, anmodningsperiodeService, gsakFasade);
    }

    @Test
    public void finnSakOgBestemRuting_anmodningsperiodeUtenSvarFinnes_verifiserKorrektResultat() throws Exception {

        when(fagsakService.finnFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak(Behandlingstyper.SOEKNAD, Behandlingsstatus.ANMODNING_UNNTAK_SENDT)));
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singleton(new Anmodningsperiode()));
        Prosessinstans prosessinstans = hentProsessinstans();
        RutingResultat resultat = svarAnmodningUnntakInitialiserer.finnSakOgBestemRuting(prosessinstans, 1L);

        assertThat(resultat).isEqualTo(RutingResultat.OPPDATER_BEHANDLING);
        assertThat(prosessinstans.getBehandling()).isNotNull();
    }

    @Test
    public void finnSakOgBestemRuting_anmodningsperiodeSvarRegistrert_verifiserKorrektResultat() throws Exception {

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());

        when(fagsakService.finnFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak(Behandlingstyper.SOEKNAD, Behandlingsstatus.ANMODNING_UNNTAK_SENDT)));
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singleton(anmodningsperiode));
        Prosessinstans prosessinstans = hentProsessinstans();
        RutingResultat resultat = svarAnmodningUnntakInitialiserer.finnSakOgBestemRuting(prosessinstans, 1L);

        assertThat(resultat).isEqualTo(RutingResultat.INGEN_BEHANDLING);
        assertThat(prosessinstans.getBehandling()).isNotNull();
    }

    @Test
    public void finnSakOgBestemRuting_behandlingstypeSøknadIkkeYrkesaktiv_oppgaveOppdateres() throws Exception {
        Fagsak fagsak = hentFagsak(Behandlingstyper.SOEKNAD_IKKE_YRKESAKTIV, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        when(fagsakService.finnFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(fagsak));
        Prosessinstans prosessinstans = hentProsessinstans();
        when(gsakFasade.finnOppgaverMedSaksnummer(eq(fagsak.getSaksnummer())))
            .thenReturn(Collections.singletonList(new Oppgave.Builder().build()));
        RutingResultat resultat = svarAnmodningUnntakInitialiserer.finnSakOgBestemRuting(prosessinstans, 1L);

        verify(gsakFasade).oppdaterOppgave(any(), any(OppgaveOppdatering.class));
        assertThat(resultat).isEqualTo(RutingResultat.INGEN_BEHANDLING);
        assertThat(prosessinstans.getBehandling()).isNotNull();
    }

    @Test(expected = FunksjonellException.class)
    public void finnSakOgBestemRuting_ingenAnmodningsperiode_forventException() throws Exception {

        when(fagsakService.finnFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak(Behandlingstyper.SOEKNAD, Behandlingsstatus.FORELOEPIG_LOVVALG)));
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.emptyList());
        Prosessinstans prosessinstans = hentProsessinstans();
        svarAnmodningUnntakInitialiserer.finnSakOgBestemRuting(prosessinstans, 1L);
    }

    @Test(expected = TekniskException.class)
    public void finnSakOgBestemRuting_korrektBehandlingsstatusIngenFagsak_forventException() throws Exception {

        when(fagsakService.finnFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.empty());
        Prosessinstans prosessinstans = hentProsessinstans();
        svarAnmodningUnntakInitialiserer.finnSakOgBestemRuting(prosessinstans, 1L);
    }

    @Test
    public void hentAktuellProsessType() {
        assertThat(svarAnmodningUnntakInitialiserer.hentAktuellProsessType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK_SVAR);
    }

    private Fagsak hentFagsak(Behandlingstyper behandlingstype, Behandlingsstatus behandlingsstatus) {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setType(behandlingstype);
        behandling.setStatus(behandlingsstatus);
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        Aktoer aktoer = new Aktoer();
        aktoer.setAktørId(AKTØR_ID);
        fagsak.setAktører(Sets.newSet(aktoer));
        return fagsak;
    }

    private Prosessinstans hentProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType("A002");
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        return prosessinstans;
    }
}