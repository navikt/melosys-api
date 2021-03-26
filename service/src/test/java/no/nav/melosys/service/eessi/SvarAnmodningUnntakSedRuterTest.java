package no.nav.melosys.service.eessi;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.eessi.ruting.SvarAnmodningUnntakSedRuter;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class SvarAnmodningUnntakSedRuterTest {

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private OppgaveService oppgaveService;

    private SvarAnmodningUnntakSedRuter svarAnmodningUnntakSedRuter;

    private final String aktørID = "223345325342";

    @BeforeEach
    public void setUp() {
        svarAnmodningUnntakSedRuter = new SvarAnmodningUnntakSedRuter(prosessinstansService, fagsakService, anmodningsperiodeService, oppgaveService);
    }

    @Test
    void finnSakOgBestemRuting_anmodningsperiodeUtenSvarFinnes_verifiserKorrektResultat() throws Exception {

        Fagsak fagsak = hentFagsak(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        when(fagsakService.hentFagsakFraArkivsakID(anyLong())).thenReturn(fagsak);
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singleton(new Anmodningsperiode()));
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding eessiMelding = melosysEessiMelding();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding);
        svarAnmodningUnntakSedRuter.rutSedTilBehandling(prosessinstans, 1L);

        verify(prosessinstansService).opprettProsessinstansMottattSvarAnmodningUnntak(eq(fagsak.hentAktivBehandling()), eq(eessiMelding));
    }

    @Test
    void finnSakOgBestemRuting_anmodningsperiodeSvarRegistrert_verifiserKorrektResultat() throws Exception {

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());

        Fagsak fagsak = hentFagsak(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);

        when(fagsakService.hentFagsakFraArkivsakID(anyLong())).thenReturn(fagsak);
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singleton(anmodningsperiode));
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding eessiMelding = melosysEessiMelding();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding);

        svarAnmodningUnntakSedRuter.rutSedTilBehandling(prosessinstans, 1L);

        verify(prosessinstansService).opprettProsessinstansSedJournalføring(
            eq(fagsak.hentAktivBehandling()), eq(eessiMelding)
        );
    }

    @Test
    void finnSakOgBestemRuting_behandlingstypeSøknadIkkeYrkesaktiv_oppgaveOppdateres() throws Exception {
        Fagsak fagsak = hentFagsak(Behandlingstema.IKKE_YRKESAKTIV, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        when(fagsakService.hentFagsakFraArkivsakID(anyLong())).thenReturn(fagsak);
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding eessiMelding = melosysEessiMelding();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding);

        when(oppgaveService.finnOppgaveMedFagsaksnummer(eq(fagsak.getSaksnummer())))
            .thenReturn(Optional.of(new Oppgave.Builder().build()));

        svarAnmodningUnntakSedRuter.rutSedTilBehandling(prosessinstans, 1L);

        verify(oppgaveService).oppdaterOppgave(any(), any(OppgaveOppdatering.class));
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(
            eq(fagsak.hentAktivBehandling()), eq(eessiMelding)
        );
    }

    @Test
    void finnSakOgBestemRuting_ingenAnmodningsperiode_forventException() throws Exception {

        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding eessiMelding = melosysEessiMelding();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding);

        when(fagsakService.hentFagsakFraArkivsakID(anyLong())).thenReturn(hentFagsak(Behandlingstema.UTSENDT_SELVSTENDIG, Behandlingsstatus.FORELOEPIG_LOVVALG));
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.emptyList());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> svarAnmodningUnntakSedRuter.rutSedTilBehandling(prosessinstans, 1L))
            .withMessageContaining("men behandlingen har ingen anmodningsperiode");
    }

    private Fagsak hentFagsak(Behandlingstema behandlingstema, Behandlingsstatus behandlingsstatus) {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setTema(behandlingstema);
        behandling.setStatus(behandlingsstatus);
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId(aktørID);
        fagsak.setAktører(Sets.newSet(aktoer));
        return fagsak;
    }

    private MelosysEessiMelding melosysEessiMelding() {
        MelosysEessiMelding eessiMelding = new MelosysEessiMelding();
        eessiMelding.setSedType("A002");
        return eessiMelding;
    }
}