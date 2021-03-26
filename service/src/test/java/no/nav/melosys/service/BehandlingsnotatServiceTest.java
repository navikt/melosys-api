package no.nav.melosys.service;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.behandling.Behandlingsnotat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingsnotatRepository;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingsnotatServiceTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsnotatRepository behandlingsnotatRepository;

    private BehandlingsnotatService behandlingsnotatService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<Behandlingsnotat> captor;

    private Fagsak fagsak;

    private final String saksnummer = "MEL-123";

    @Before
    public void setup() throws IkkeFunnetException {
        behandlingsnotatService = new BehandlingsnotatService(behandlingsnotatRepository, fagsakService);

        fagsak = new Fagsak();
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    public void opprettNotat_fagsakHarIkkeAktivBehandling_forventException() throws FunksjonellException, TekniskException {
        lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("har ingen aktive behandlinger");
        behandlingsnotatService.opprettNotat(saksnummer, "heihei");
    }

    @Test
    public void opprettNotat_fagsakHarAktivBehandling_blirLagret() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling(fagsak, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);

        String tekst = "heiheihei";
        behandlingsnotatService.opprettNotat(saksnummer, tekst);
        verify(behandlingsnotatRepository).save(captor.capture());

        assertThat(captor.getValue().getBehandling()).isEqualTo(behandling);
    }

    @Test
    public void hentNotaterForFagsak_enBehandlingErAvsluttet_verifiserRedigerbareOgIkkeRedigerbareNotater() throws IkkeFunnetException {
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
    public void oppdaterNotat_behandlingIkkeRedigerbar_kasterException() throws FunksjonellException {
        final long notatID = 111L;
        Behandling behandling = lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET);
        Behandlingsnotat behandlingsnotat = new Behandlingsnotat();
        behandlingsnotat.setId(notatID);
        behandlingsnotat.setBehandling(behandling);
        behandlingsnotat.setRegistrertAv("Z");

        when(behandlingsnotatRepository.findById(eq(notatID))).thenReturn(Optional.of(behandlingsnotat));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage(" kan ikke oppdateres, da den tilhører en behandling som er avsluttet");

        behandlingsnotatService.oppdaterNotat(notatID, "Et skummelt notat.");
    }

    @Test
    public void oppdaterNotat_behandlingSaksbehandlerIkkeTilgang_kasterException() throws FunksjonellException {
        final long notatID = 111L;
        Behandling behandling = lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING);
        Behandlingsnotat behandlingsnotat = new Behandlingsnotat();
        behandlingsnotat.setId(notatID);
        behandlingsnotat.setBehandling(behandling);
        behandlingsnotat.setRegistrertAv("Z-ukjent");

        when(behandlingsnotatRepository.findById(eq(notatID))).thenReturn(Optional.of(behandlingsnotat));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Et notat kan ikke endres av andre!");

        behandlingsnotatService.oppdaterNotat(notatID, "Et enda skumlere notat.");
    }

    private Behandling lagBehandling(Fagsak fagsak, Behandlingsstatus behandlingsstatus) {
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setStatus(behandlingsstatus);
        fagsak.getBehandlinger().add(behandling);
        return behandling;
    }

}