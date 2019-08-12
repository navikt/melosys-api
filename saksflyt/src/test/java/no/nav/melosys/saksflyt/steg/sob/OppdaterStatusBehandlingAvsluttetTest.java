package no.nav.melosys.saksflyt.steg.sob;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterStatusBehandlingAvsluttetTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private SakOgBehandlingFasade sakOgBehandlingFasade;

    private OppdaterStatusBehandlingAvsluttet agent;

    @Before
    public void setUp() {
        agent = new OppdaterStatusBehandlingAvsluttet(sakOgBehandlingFasade);
    }

    @Test
    public void utførSteg_aktørIdFinnes_kallerSakOgBehandling() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        Behandling b = lagBehandling();
        p.setBehandling(b);
        agent.utfør(p);
        verify(sakOgBehandlingFasade).sendBehandlingAvsluttet(any(BehandlingStatusMapper.class));
    }

    @Test
    public void utførSteg_aktørFinnesIkke_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        Behandling b = lagBehandling();
        b.getFagsak().getAktører().clear();
        p.setBehandling(b);

        expectedException.expect(FunksjonellException.class);
        agent.utfør(p);

        verify(sakOgBehandlingFasade, never()).sendBehandlingAvsluttet(any(BehandlingStatusMapper.class));
    }

    @Test
    public void utførSteg_ingenAktørID_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        Behandling b = lagBehandling();
        b.getFagsak().hentBruker().setAktørId(null);
        p.setBehandling(b);

        expectedException.expect(FunksjonellException.class);
        agent.utfør(p);

        verify(sakOgBehandlingFasade, never()).sendBehandlingAvsluttet(any(BehandlingStatusMapper.class));
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        Set<Aktoer> aktører = new HashSet<>(Collections.singleton(lagAktørBruker()));
        fagsak.setAktører(aktører);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.KLAGE);
        behandling.setId(123L);

        return behandling;
    }

    private static Aktoer lagAktørBruker() {
        Aktoersroller type = Aktoersroller.BRUKER;
        Aktoer aktør = new Aktoer();
        aktør.setAktørId(type.name());
        aktør.setAktørId("123");
        aktør.setRolle(type);
        return aktør;
    }
}