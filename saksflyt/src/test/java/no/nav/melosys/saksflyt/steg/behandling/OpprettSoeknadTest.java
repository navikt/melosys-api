package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
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

    private final long behandlingID = 12321L;

    @BeforeEach
    public void setUp() {
        opprettSoeknad = new OpprettSoeknad(behandlingsgrunnlagService);
    }

    @Test
    void utfør_behandlingstemaUtsendtArbeidstaker_oppretterSøknad() throws FunksjonellException {
        opprettSoeknad.utfør(lagProsessinstans(Behandlingstema.UTSENDT_ARBEIDSTAKER));
        verify(behandlingsgrunnlagService).opprettSøknadGrunnlag(eq(behandlingID), any(SoeknadDokument.class));
    }

    @Test
    void utfør_behandlingsTemaØvrigeSed_oppretterIkkeSøknad() throws FunksjonellException {
        opprettSoeknad.utfør(lagProsessinstans(Behandlingstema.ØVRIGE_SED_MED));
        verify(behandlingsgrunnlagService, never()).opprettSøknadGrunnlag(eq(behandlingID), any(SoeknadDokument.class));
    }

    private Prosessinstans lagProsessinstans(Behandlingstema behandlingstema) {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setTema(behandlingstema);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        return prosessinstans;
    }
}