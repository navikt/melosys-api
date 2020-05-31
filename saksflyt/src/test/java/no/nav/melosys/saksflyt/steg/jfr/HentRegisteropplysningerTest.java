package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HentRegisteropplysningerTest {

    @Mock
    private RegisteropplysningerService registeropplysningerService;

    private HentRegisteropplysninger agent;

    @Before
    public void setUp() {
        agent = new HentRegisteropplysninger(registeropplysningerService);
    }

    @Test
    public void utførSteg() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(222L);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);
        p.setData(ProsessDataKey.BRUKER_ID, "99999999991");
        p.setData(ProsessDataKey.SØKNADSPERIODE, new Periode(LocalDate.now(), LocalDate.now()));

        agent.utfør(p);

        verify(registeropplysningerService).hentOgLagreOpplysninger(any());
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_VURDER_INNGANGSVILKÅR);
    }
}
