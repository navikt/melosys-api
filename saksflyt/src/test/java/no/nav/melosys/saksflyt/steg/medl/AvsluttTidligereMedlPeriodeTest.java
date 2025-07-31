package no.nav.melosys.saksflyt.steg.medl;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AvsluttTidligereMedlPeriodeTest {

    @Mock
    private MedlPeriodeService medlPeriodeService;

    private AvsluttTidligereMedlPeriode avsluttTidligereMedlPeriode;

    @BeforeEach
    public void setUp() {
        avsluttTidligereMedlPeriode = new AvsluttTidligereMedlPeriode(medlPeriodeService);
    }

    @Test
    public void utfør_ikkeEndring_verifiserLagreLovvalgspeirode() throws Exception {

        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .build();

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, false);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "12312322");

        avsluttTidligereMedlPeriode.utfør(prosessinstans);
    }

    @Test
    public void utfør_erEndring_verifiserAvsluttTidligereMedlPeriode() {

        Fagsak fagsak = hentFagsak();
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medFagsak(fagsak)
            .build();

        Prosessinstans prosessinstans = hentProsessinstans(behandling, true);
        avsluttTidligereMedlPeriode.utfør(prosessinstans);
        verify(medlPeriodeService).avsluttTidligerMedlPeriode(FagsakTestFactory.SAKSNUMMER);
    }

    private Fagsak hentFagsak() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medRegistrertDato(Instant.now())
            .medStatus(Behandlingsstatus.AVSLUTTET)
            .build();

        return FagsakTestFactory.builder().behandlinger(behandling).build();
    }

    private Prosessinstans hentProsessinstans(Behandling behandling, boolean erEndring) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, erEndring);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "12312322");
        return prosessinstans;
    }
}
