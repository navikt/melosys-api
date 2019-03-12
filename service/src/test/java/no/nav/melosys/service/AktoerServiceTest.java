package no.nav.melosys.service;

import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.AktoerRepository;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AktoerServiceTest {

    @Mock
    private AktoerRepository aktoerRepository;

    private AktoerService aktoerService;

    @Before
    public void setUp() {
        aktoerService = Mockito.spy(new AktoerService(aktoerRepository));
    }

    @Test
    public final void lagOppdaterAktoer() throws FunksjonellException {
        AktoerDto aktoerRepDto = new AktoerDto();
        aktoerRepDto.setAktoerID("1234");
        aktoerRepDto.setRolleKode("BRUKER");

        when(aktoerRepository.findByFagsakAndRolleAndRepresenterer(any(), any(), any())).thenReturn(Optional.empty());
        aktoerService.lagEllerOppdaterAktoer(lagFagsak(), aktoerRepDto);
        verify(aktoerRepository, atLeast(1)).save(any());

        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId("1235");

        when(aktoerRepository.findByFagsakAndRolleAndRepresenterer(any(), any(), any())).thenReturn(Optional.of(aktoer));

        Aktoer aktoerFraApiLag = new Aktoer();
        aktoerFraApiLag.setRolle(Aktoersroller.BRUKER);
        aktoerService.lagEllerOppdaterAktoer(lagFagsak(), aktoerRepDto);
        verify(aktoerRepository, atLeast(1)).deleteById(aktoer);
        verify(aktoerRepository, atLeast(1)).save(any());

    }

    @Test
    public final void hentfagsakAktoerer() throws IkkeFunnetException {

        Aktoer aktoer = new Aktoer();

        when(aktoerRepository.findByFagsakAndRolleAndRepresenterer(any(), any(), any())).thenReturn(Optional.of( aktoer));

        aktoerService.hentfagsakAktoerer(lagFagsak(), "MYNDIGHET", null);

        verify(aktoerRepository).findByFagsakAndRolleAndRepresenterer(eq(lagFagsak()), eq(Aktoersroller.MYNDIGHET), eq(null));

        aktoerService.hentfagsakAktoerer(lagFagsak(), "REPRESENTANT", "BRUKER");

        verify(aktoerRepository).findByFagsakAndRolleAndRepresenterer(eq(lagFagsak()), eq(Aktoersroller.REPRESENTANT), eq(Representerer.BRUKER));
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-1");
        return fagsak;
    }
}
