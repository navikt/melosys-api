package no.nav.melosys.saksflyt.steg.msa;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendForvaltningsmeldingTest {

    @Mock
    private BrevBestiller brevBestiller;
    @Mock
    private BehandlingService behandlingService;

    private SendForvaltningsmelding sendForvaltningsmelding;

    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final Behandling behandling = new Behandling();

    @Before
    public void setup() throws IkkeFunnetException {
        sendForvaltningsmelding = new SendForvaltningsmelding(brevBestiller, behandlingService);

        prosessinstans.setBehandling(behandling);
        behandling.setId(3L);
        when(behandlingService.hentBehandling(eq(behandling.getId()))).thenReturn(behandling);
    }

    @Test
    public void utfør() throws MelosysException {
        sendForvaltningsmelding.utfør(prosessinstans);
        verify(brevBestiller).bestill(
            eq(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID),
            isNull(),
            eq(Mottaker.av(Aktoersroller.BRUKER)),
            eq(behandling)
        );
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }
}