package no.nav.melosys.saksflyt.steg.vs;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.VS_SEND_SOKNAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendOrienteringsbrevTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BrevBestiller brevBestiller;

    private SendOrienteringsbrev steg;
    private Behandling behandling;
    private Prosessinstans prosessinstans;

    @Captor
    private ArgumentCaptor<Brevbestilling> captor;


    @Before
    public void setup() throws IkkeFunnetException {
        steg = new SendOrienteringsbrev(behandlingService , brevBestiller);

        behandling = new Behandling();
        behandling.setId(1L);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
    }

    @Test
    public void utfør_brevbestilling_harRiktigBrevTypeOgMottaker() throws FunksjonellException, TekniskException {
        steg.utfør(prosessinstans);
        verify(brevBestiller).bestill(captor.capture());
        Brevbestilling brevbestilling = captor.getValue();
        assertThat(brevbestilling.getDokumentType()).isEqualTo(Produserbaredokumenter.ORIENTERING_VIDERESENDT_SOEKNAD);
        assertThat(brevbestilling.getMottakere().stream().map(Mottaker::getRolle)).containsExactly(Aktoersroller.BRUKER);
        assertThat(brevbestilling.getBehandling()).isEqualTo(behandling);
    }

    @Test
    public void utfør_setNesteSteg_erSendSøknad() throws FunksjonellException, TekniskException {
        steg.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(VS_SEND_SOKNAD);
    }
}