package no.nav.melosys.saksflyt.steg.sob;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.service.BehandlingService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.JFR_HENT_PERS_OPPL;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.JFR_OPPDATER_SAKSRELASJON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterStatusBehandlingOpprettetTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private SakOgBehandlingFasade sakOgBehandlingFasade;
    @Mock
    private TpsService tpsService;
    @Mock
    private BehandlingService behandlingService;

    private OppdaterStatusBehandlingOpprettet agent;

    @Before
    public void setUp() {
        agent = new OppdaterStatusBehandlingOpprettet(sakOgBehandlingFasade, tpsService, behandlingService);
    }

    @Test
    public void utførSteg_alltid_kallerSakOgBehandling() throws TekniskException {
        Prosessinstans p = new Prosessinstans();
        Behandling b = lagBehandling();
        p.setBehandling(b);
        agent.utfør(p);
        verify(sakOgBehandlingFasade).sendBehandlingOpprettet(any(BehandlingStatusMapper.class));
        assertThat(p.getSteg()).isEqualTo(JFR_OPPDATER_SAKSRELASJON);
    }

    @Test
    public void utførSteg_typeNySakFraDok_tilHentSaksopplysninger() throws TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.OPPRETT_NY_SAK);
        Behandling b = lagBehandling();
        p.setBehandling(b);
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(JFR_HENT_PERS_OPPL);
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        Set<Aktoer> aktører = new HashSet<>(Collections.singleton(lagAktørBruker()));
        fagsak.setAktører(aktører);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.SOEKNAD);
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