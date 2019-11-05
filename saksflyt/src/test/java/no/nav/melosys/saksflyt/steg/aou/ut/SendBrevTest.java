package no.nav.melosys.saksflyt.steg.aou.ut;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendBrevTest {

    @Mock
    private BrevBestiller brevBestiller;

    @Mock
    private BehandlingRepository behandlingRepository;

    private Prosessinstans p;
    private SendBrev agent;

    private static final String SAKSBEHANDLER = "Z121212";

    @Before
    public void setUp() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setId(1L);

        when(behandlingRepository.findWithSaksopplysningerById(any())).thenReturn(behandling);

        p = new Prosessinstans();
        p.setBehandling(behandling);
        p.setType(ProsessType.ANMODNING_OM_UNNTAK);
        p.setData(ProsessDataKey.SAKSBEHANDLER, "Z999");
        p.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, Behandlingsresultattyper.ANMODNING_OM_UNNTAK.getKode());

        agent = new SendBrev(brevBestiller, behandlingRepository);
    }

    @Test
    public void utfoerSteg() throws FunksjonellException, TekniskException {
        agent.utførSteg(p);
        brevBestiller.bestill(eq(Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK), eq(SAKSBEHANDLER), eq(Mottaker.av(Aktoersroller.BRUKER)), any(Behandling.class));
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.AOU_SEND_SED);
    }
}