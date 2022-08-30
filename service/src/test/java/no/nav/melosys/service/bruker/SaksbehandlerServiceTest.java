package no.nav.melosys.service.bruker;

import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaksbehandlerServiceTest {

    //TODO: Fiks tester for SaksbehandlerService
    private final String melosysAdGruppe = "MELOSY123";
    private final String ident = "Z990007";

    private SaksbehandlerService saksbehandlerService;

    @BeforeEach
    public void setup() {
        saksbehandlerService = new SaksbehandlerService( melosysAdGruppe);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }
}
