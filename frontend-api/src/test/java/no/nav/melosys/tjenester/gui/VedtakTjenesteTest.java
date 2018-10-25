package no.nav.melosys.tjenester.gui;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.vedtak.VedtakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VedtakTjenesteTest {

    @Mock
    private VedtakService vedtakService;

    @Mock
    private Tilgang tilgang;

    private VedtakTjeneste vedtakTjeneste;

    @Before
    public void setUp() {
        vedtakTjeneste = new VedtakTjeneste(vedtakService, tilgang);
    }

    @Test
    public void fattVedtak() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        long behandlingID = 3;
        vedtakTjeneste.fattVedtak(behandlingID);

        verify(tilgang, times(1)).sjekk(behandlingID);
        verify(vedtakService, times(1)).fattVedtak(behandlingID);
    }
}