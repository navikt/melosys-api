package no.nav.melosys.saksflyt.steg.aou.inn;

import java.time.Instant;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttTidligerePeriodeTest {

    @Mock
    private OppdaterMedlFelles oppdaterMedlFelles;

    private AvsluttTidligerePeriode avsluttTidligerePeriode;

    @Before
    public void setUp() {
        avsluttTidligerePeriode = new AvsluttTidligerePeriode(oppdaterMedlFelles);
    }

    @Test
    public void utførSteg_ikkeEndring_verifiserLagreLovvalgspeirode() throws Exception {

        Behandling behandling = new Behandling();
        behandling.setId(1L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, false);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "12312322");

        avsluttTidligerePeriode.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_HENT_REGISTEROPPLYSNINGER);
    }

    @Test
    public void utførSteg_erEndring_verifiserAvsluttTidligereMedlPeriode() throws Exception {

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(hentFagsak());

        Prosessinstans prosessinstans = hentProsessinstans(behandling, true);
        avsluttTidligerePeriode.utfør(prosessinstans);
        verify(oppdaterMedlFelles).avsluttTidligerMedlPeriode(any(Fagsak.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_HENT_REGISTEROPPLYSNINGER);
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