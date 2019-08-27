package no.nav.melosys.saksflyt.steg.aou;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendBrevTest {

    @Mock
    private BrevBestiller brevBestiller;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;

    private Prosessinstans p;
    private SendBrev agent;

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

        agent = new SendBrev(brevBestiller, behandlingRepository, anmodningsperiodeService);
    }

    @Test
    public void utfoerSteg() throws FunksjonellException {
        agent.utførSteg(p);
        verify(anmodningsperiodeService).oppdaterAnmodningsperiodeSendtForBehandling(anyLong());
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.AOU_SEND_SED);
    }

    @Test
    public void utførStegAntallSendteBrev() throws FunksjonellException, TekniskException {
        agent.utførSteg(p);
        verify(brevBestiller).bestill(eq(Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK), anyString(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(Behandling.class));
        verify(brevBestiller).bestill(eq(Produserbaredokumenter.ANMODNING_UNNTAK), anyString(), eq(Mottaker.av(Aktoersroller.MYNDIGHET)), any(Behandling.class));
    }
}