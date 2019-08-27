package no.nav.melosys.saksflyt.steg.aou.mottak.svar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.FagsakOgBehandlingFelles;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttFagsakOgBehandlingTest {

    @Mock
    private FagsakOgBehandlingFelles fagsakOgBehandlingFelles;

    @InjectMocks
    private AvsluttFagsakOgBehandling avsluttFagsakOgBehandling;

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setFagsak(new Fagsak());

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        avsluttFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakOgBehandlingFelles).avsluttFagsakOgBehandling(eq(behandling), eq(Behandlingsresultattyper.ANMODNING_OM_UNNTAK));
    }
}