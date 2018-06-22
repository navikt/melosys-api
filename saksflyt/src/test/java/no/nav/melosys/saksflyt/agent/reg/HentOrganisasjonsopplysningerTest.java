package no.nav.melosys.saksflyt.agent.reg;

import java.util.HashSet;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class HentOrganisasjonsopplysningerTest {

    @Mock
    private EregFasade eregFasade;

    private HentOrganisasjonsopplysninger agent;

    @Before
    public void setUp() {
        FagsakService fagsakService = mock(FagsakService.class);
        agent = new HentOrganisasjonsopplysninger(fagsakService, eregFasade);
    }

    @Test
    public void utfoerSteg() throws SikkerhetsbegrensningException, IkkeFunnetException {
        Prosessinstans p = new Prosessinstans();

        p.setBehandling(new Behandling());
        p.getBehandling().setSaksopplysninger(new HashSet<>());

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_MEDL_OPPL);
    }
}
