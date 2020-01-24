package no.nav.melosys.saksflyt.steg.ul;

import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@Ignore // FIXME: Legg til tester etter refactoring av UtpekAnnetLandSendSed
@RunWith(MockitoJUnitRunner.class)
public class UtpekAnnetLandSendSedTest {

    @Mock
    private EessiService eessiService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    private UtpekAnnetLandSendSed utpekAnnetLandSendSed;

    @Before
    public void settOpp() {
        utpekAnnetLandSendSed = new UtpekAnnetLandSendSed(eessiService, fagsakService, joarkFasade, tpsFasade,
            utenlandskMyndighetService, prosessinstansRepository);
    }
}
