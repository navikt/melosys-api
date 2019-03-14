package no.nav.melosys.service;

import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.AktoerRepository;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AktoerServiceTest {

    @Mock
    private AktoerRepository aktoerRepository;

    private AktoerService aktoerService;

    @Captor
    private ArgumentCaptor<Example> exampleCaptor;

    @Before
    public void setUp() {
        aktoerService = new AktoerService(aktoerRepository);
    }

    @Test
    public final void lagEllerOppdater_nyAktoer() throws FunksjonellException {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID("1234");
        aktoerDto.setRolleKode("BRUKER");

        when(aktoerRepository.findByFagsakAndRolleAndRepresenterer(any(), any(), any())).thenReturn(Optional.empty());
        aktoerService.lagEllerOppdaterAktoer(lagFagsak(), aktoerDto);
        verify(aktoerRepository).save(any(Aktoer.class));
    }

    @Test
    public final void lagEllerOppdater_oppdaterAktoer() throws FunksjonellException {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID("1234");
        aktoerDto.setRolleKode("BRUKER");

        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId("1235");

        when(aktoerRepository.findByFagsakAndRolleAndRepresenterer(any(), any(), any())).thenReturn(Optional.of(aktoer));

        Aktoer aktoerFraApiLag = new Aktoer();
        aktoerFraApiLag.setRolle(Aktoersroller.BRUKER);
        aktoerService.lagEllerOppdaterAktoer(lagFagsak(), aktoerDto);
        verify(aktoerRepository).deleteById(aktoer);
        verify(aktoerRepository).save(any(Aktoer.class));

    }

    @Test
    public final void hentfagsakAktoerer() {
        aktoerService.hentfagsakAktører(lagFagsak(), Aktoersroller.REPRESENTANT, Representerer.BRUKER);

        verify(aktoerRepository).findAll(exampleCaptor.capture());
        Example aktørExample = exampleCaptor.getValue();

        Aktoer aktørProbe = (Aktoer) aktørExample.getProbe();
        assertThat(aktørProbe.getFagsak()).isEqualTo(lagFagsak());
        assertThat(aktørProbe.getRolle()).isEqualTo(Aktoersroller.REPRESENTANT);
        assertThat(aktørProbe.getRepresenterer()).isEqualTo(Representerer.BRUKER);
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-1");
        return fagsak;
    }
}
