package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpprettSoeknadTest {

    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    private OpprettSoeknad opprettSoeknad;

    @BeforeEach
    public void setUp() {
        opprettSoeknad = new OpprettSoeknad(behandlingsgrunnlagService);
    }

    @Test
    void utfør() throws FunksjonellException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.JFR_NY_SAK);

        opprettSoeknad.utfør(prosessinstans);

        verify(behandlingsgrunnlagService, times(1)).opprettSøknadGrunnlag(eq(behandling.getId()), any(SoeknadDokument.class));
    }
}