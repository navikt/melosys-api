package no.nav.melosys.saksflyt.steg.behandling;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

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
    void utfør_behandlingstemaUtsendtArbeidstaker_oppretterSøknadYrkesaktiveEøs() {
        Prosessinstans prosessinstans = lagProsessinstans();

        opprettSoeknad.utfør(prosessinstans);

        verify(behandlingsgrunnlagService).opprettSøknad(prosessinstans);
    }

    private Prosessinstans lagProsessinstans() {
        Behandling behandling = new Behandling();
        long behandlingID = 12321L;
        behandling.setId(behandlingID);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.SØKNADSLAND, new SoeknadslandDto(List.of("DK", "SE"), true));
        return prosessinstans;
    }
}
