package no.nav.melosys.service;

import no.nav.freg.abac.core.annotation.context.AbacContext;
import no.nav.freg.abac.core.dto.request.XacmlRequest;
import no.nav.freg.abac.core.dto.response.Decision;
import no.nav.freg.abac.core.dto.response.XacmlResponse;
import no.nav.freg.abac.core.service.AbacService;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.abac.BehandlingTilgang;
import no.nav.melosys.service.abac.FagsakTilgang;
import no.nav.melosys.service.abac.JournalTilgang;
import no.nav.melosys.service.abac.PepAktoerOversetter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbacTest {
    private BehandlingTilgang behandlingTilgang;
    private FagsakTilgang fagsakTilgang;
    private JournalTilgang journalTilgang;

    @Mock
    private PepAktoerOversetter pep;

    private XacmlResponse abacResponse;

    @Mock
    private FagsakRepository fagsakRepository;

    private Fagsak fagsakMocked;

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AbacContext abacContext = mock(AbacContext.class);
        when(abacContext.getRequest()).thenReturn(new XacmlRequest());

        TpsFasade tpsFasade = mock(TpsFasade.class);
        when(tpsFasade.hentIdentForAktørId(any())).thenReturn("12345678910");

        abacResponse = mock(XacmlResponse.class);

        AbacService abacService = mock(AbacService.class);
        when(abacService.evaluate(any())).thenReturn(abacResponse);

        pep = new PepAktoerOversetter(tpsFasade, abacService, abacContext);

        fagsakMocked = mock(Fagsak.class);
        when(fagsakMocked.hentAktørMedRolleType(any())).thenReturn(new Aktoer());

        behandlingTilgang = new BehandlingTilgang(fagsakRepository, pep);
        fagsakTilgang = new FagsakTilgang(fagsakRepository, pep);

        JoarkFasade joarkFasade = mock(JoarkFasade.class);
        when(joarkFasade.hentJournalpost(any())).thenReturn(mock(Journalpost.class));
        journalTilgang = new JournalTilgang(joarkFasade, pep);
    }

    @Test
    public void testPepOversetter() throws SikkerhetsbegrensningException, IkkeFunnetException {
        Aktoer aktør = new Aktoer();
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        pep.sjekkTilgangTil(aktør);
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testPepOversetterIkketilgang() throws SikkerhetsbegrensningException, IkkeFunnetException {
        Aktoer aktør = new Aktoer();
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        pep.sjekkTilgangTil(aktør);
    }



    @Test(expected = SikkerhetsbegrensningException.class)
    public void testBehandlingsIdIkkeKnyttetTilFagsak() throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        behandlingTilgang.sjekk(102323934);
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testBehandlingsIdIkketilgang() throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);

        List<Fagsak> fagsaker = Arrays.asList(fagsakMocked);
        when(fagsakRepository.findByBehandlingsId(anyLong())).thenReturn(fagsaker);

        behandlingTilgang.sjekk(102323934);
    }

    @Test
    public void testBehandlingsIdOk() throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);

        List<Fagsak> fagsaker = Arrays.asList(fagsakMocked);
        when(fagsakRepository.findByBehandlingsId(anyLong())).thenReturn(fagsaker);

        behandlingTilgang.sjekk(102323934);
    }

    @Test
    public void testFagsakOk() throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        fagsakTilgang.sjekk(fagsakMocked);
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testFagsakIkkeTilgang() throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        fagsakTilgang.sjekk(fagsakMocked);
    }

    @Test
    public void testJournalOk() throws SikkerhetsbegrensningException, IntegrasjonException {
        when(abacResponse.getDecision()).thenReturn(Decision.PERMIT);
        journalTilgang.sjekk("1234567");
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void testJournalIkkeTilgang() throws SikkerhetsbegrensningException, IntegrasjonException {
        when(abacResponse.getDecision()).thenReturn(Decision.DENY);
        journalTilgang.sjekk("1234567");
    }
}
