package no.nav.melosys.saksflyt.agent.reg;

import java.util.HashSet;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HentOrganisasjonsopplysningerTest {

    @Mock
    private EregFasade eregFasade;
    
    @Mock
    private SaksopplysningRepository soppRepo;

    @Mock
    private BehandlingRepository behRepo;
    
    private HentOrganisasjonsopplysninger agent;

    @Before
    public void setUp() {
        agent = new HentOrganisasjonsopplysninger(behRepo, soppRepo, eregFasade);
    }

    @Test
    public void utfoerSteg() throws SikkerhetsbegrensningException, IkkeFunnetException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setSaksopplysninger(new HashSet<>());

        when(behRepo.findOne(any())).thenReturn(p.getBehandling());
        
        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_MEDL_OPPL);
    }
}
