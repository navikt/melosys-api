package no.nav.melosys.service;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsnotat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingsnotatRepository;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BehandlingsnotatServiceTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsnotatRepository behandlingsnotatRepository;

    private BehandlingsnotatService behandlingsnotatService;

    @Captor
    private ArgumentCaptor<Behandlingsnotat> captor;

    private Fagsak fagsak;

    private final String saksnummer = "MEL-123";

    @BeforeEach
    public void setup() throws IkkeFunnetException {
        behandlingsnotatService = new BehandlingsnotatService(behandlingsnotatRepository, fagsakService);
        fagsak = new Fagsak();
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void opprettNotat_fagsakHarIkkeAktivBehandling_forventException() throws FunksjonellException, TekniskException {
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);
        lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingsnotatService.opprettNotat(saksnummer, "heihei"))
            .withMessageContaining("har ingen aktive behandlinger");
    }

    @Test
    void opprettNotat_fagsakHarAktivBehandling_blirLagret() throws FunksjonellException, TekniskException {
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);
        Behandling behandling = lagBehandling(fagsak, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);

        String tekst = "heiheihei";
        behandlingsnotatService.opprettNotat(saksnummer, tekst);
        verify(behandlingsnotatRepository).save(captor.capture());

        assertThat(captor.getValue().getBehandling()).isEqualTo(behandling);
    }

    @Test
    void hentNotaterForFagsak_enBehandlingErAvsluttet_verifiserRedigerbareOgIkkeRedigerbareNotater() throws IkkeFunnetException {
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);
        Behandling avsluttetBehandling = lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET);
        Behandlingsnotat ikkeAktivBehandlingsnotat = new Behandlingsnotat();
        ikkeAktivBehandlingsnotat.setTekst("tetetetekksttt");
        ikkeAktivBehandlingsnotat.setBehandling(avsluttetBehandling);
        avsluttetBehandling.getBehandlingsnotater().add(ikkeAktivBehandlingsnotat);

        Behandling aktivBehandling = lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING);
        Behandlingsnotat aktivBehandlingsnotat = new Behandlingsnotat();
        aktivBehandlingsnotat.setTekst("tkkkkkkk");
        aktivBehandlingsnotat.setBehandling(aktivBehandling);
        aktivBehandling.getBehandlingsnotater().add(aktivBehandlingsnotat);

        assertThat(ikkeAktivBehandlingsnotat.erRedigerbar()).isFalse();
        assertThat(aktivBehandling.erRedigerbar()).isTrue();

        Collection<Behandlingsnotat> notater = behandlingsnotatService.hentNotatForFagsak(saksnummer);
        assertThat(notater).containsExactlyInAnyOrder(ikkeAktivBehandlingsnotat, aktivBehandlingsnotat);
    }

    @Test
    void oppdaterNotat_behandlingIkkeRedigerbar_kasterException() throws FunksjonellException {
        final long notatID = 111L;
        Behandling behandling = lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET);
        Behandlingsnotat behandlingsnotat = new Behandlingsnotat();
        behandlingsnotat.setId(notatID);
        behandlingsnotat.setBehandling(behandling);
        behandlingsnotat.setRegistrertAv("Z");

        when(behandlingsnotatRepository.findById(eq(notatID))).thenReturn(Optional.of(behandlingsnotat));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingsnotatService.oppdaterNotat(notatID, "Et skummelt notat."))
            .withMessageContaining(" kan ikke oppdateres, da den tilhører en behandling som er avsluttet");
    }

    @Test
    void oppdaterNotat_behandlingSaksbehandlerIkkeTilgang_kasterException() throws FunksjonellException {
        final long notatID = 111L;
        Behandling behandling = lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING);
        Behandlingsnotat behandlingsnotat = new Behandlingsnotat();
        behandlingsnotat.setId(notatID);
        behandlingsnotat.setBehandling(behandling);
        behandlingsnotat.setRegistrertAv("Z-ukjent");

        when(behandlingsnotatRepository.findById(eq(notatID))).thenReturn(Optional.of(behandlingsnotat));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingsnotatService.oppdaterNotat(notatID, "Et enda skumlere notat."))
            .withMessageContaining("Et notat kan ikke endres av andre!");
    }

    private Behandling lagBehandling(Fagsak fagsak, Behandlingsstatus behandlingsstatus) {
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setStatus(behandlingsstatus);
        fagsak.getBehandlinger().add(behandling);
        return behandling;
    }

}
