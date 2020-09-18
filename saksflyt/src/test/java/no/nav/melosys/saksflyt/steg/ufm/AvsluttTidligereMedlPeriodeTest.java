package no.nav.melosys.saksflyt.steg.ufm;

import java.time.Instant;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttTidligereMedlPeriodeTest {

    @Mock
    private MedlPeriodeService medlPeriodeService;

    private AvsluttTidligereMedlPeriode avsluttTidligereMedlPeriode;

    @Before
    public void setUp() {
        avsluttTidligereMedlPeriode = new AvsluttTidligereMedlPeriode(medlPeriodeService);
    }

    @Test
    public void utfør_ikkeEndring_verifiserLagreLovvalgspeirode() throws Exception {

        Behandling behandling = new Behandling();
        behandling.setId(1L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, false);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "12312322");

        avsluttTidligereMedlPeriode.utfør(prosessinstans);
    }

    @Test
    public void utfør_erEndring_verifiserAvsluttTidligereMedlPeriode() throws Exception {

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(hentFagsak());

        Prosessinstans prosessinstans = hentProsessinstans(behandling, true);
        avsluttTidligereMedlPeriode.utfør(prosessinstans);
        verify(medlPeriodeService).avsluttTidligerMedlPeriode(any(Fagsak.class));
    }

    private Fagsak hentFagsak() {
        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setRegistrertDato(Instant.now());
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        return fagsak;
    }

    private Prosessinstans hentProsessinstans(Behandling behandling, boolean erEndring) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, erEndring);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "12312322");
        return prosessinstans;
    }
}