package no.nav.melosys.saksflyt.steg.behandling;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
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
    void utfør_behandlingstemaUtsendtArbeidstaker_oppretterSøknadYrkesaktiveEøs() {
        opprettSoeknad.utfør(lagProsessinstans(Sakstyper.EU_EOS, Behandlingstema.UTSENDT_ARBEIDSTAKER));
        verify(behandlingsgrunnlagService).opprettSøknadYrkesaktiveEøs(eq(behandlingID), any(Soeknad.class));
    }

    @Test
    void utfør_behandlingstemaArbeidIUtlandet_oppretterSøknadFtrl() {
        opprettSoeknad.utfør(lagProsessinstans(Sakstyper.FTRL, Behandlingstema.ARBEID_I_UTLANDET));
        verify(behandlingsgrunnlagService).opprettSøknadFolketrygden(behandlingID);
    }

    @Test
    void utfør_behandlingstemaTrygdeavtaleUK_oppretterSøknadTrygdeavtale() {
        opprettSoeknad.utfør(lagProsessinstans(Sakstyper.TRYGDEAVTALE, Behandlingstema.YRKESAKTIV));
        verify(behandlingsgrunnlagService).opprettSøknadTrygdeavtale(behandlingID);
    }

    @Test
    void utfør_behandlingsTemaØvrigeSed_oppretterIkkeSøknad() {
        opprettSoeknad.utfør(lagProsessinstans(Sakstyper.EU_EOS, Behandlingstema.ØVRIGE_SED_MED));
        verify(behandlingsgrunnlagService, never()).opprettSøknadYrkesaktiveEøs(eq(behandlingID), any(Soeknad.class));
    }

    private Prosessinstans lagProsessinstans(Sakstyper sakstype, Behandlingstema behandlingstema) {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setTema(behandlingstema);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setType(sakstype);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.SØKNADSLAND, new SoeknadslandDto(List.of("DK","SE"), true));
        return prosessinstans;
    }
}
