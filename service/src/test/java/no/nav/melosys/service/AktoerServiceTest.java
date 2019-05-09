package no.nav.melosys.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AktoerRepository;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AktoerServiceTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private AktoerRepository aktørRepository;

    private AktoerService aktørService;

    @Captor
    private ArgumentCaptor<Example> exampleCaptor;
    private long aktoerId = 234L;

    @Before
    public void setUp() {
        aktørService = new AktoerService(aktørRepository);
        Aktoer aktoer = new Aktoer();
        aktoer.setId(aktoerId);
        doReturn(aktoer).when(aktørRepository).save(any());
    }

    @Test
    public final void lagEllerOppdater_nyAktoer() throws FunksjonellException {
        AktoerDto aktoerDto = spy(lagAktoerDto());
        Fagsak fagsak = lagFagsak();
        Long databaseId = aktørService.lagEllerOppdaterAktoer(fagsak, aktoerDto);

        ArgumentCaptor<Aktoer> captor = ArgumentCaptor.forClass(Aktoer.class);
        verify(aktørRepository).save(captor.capture());
        Aktoer aktoer = captor.getValue();

        assertAktoerData(aktoerDto, fagsak, aktoer);
        assertThat(aktoer.getId()).isNull();
        assertThat(databaseId).isEqualTo(aktoerId);
    }

    @Test
    public final void lagEllerOppdater_oppdaterAktoer() throws FunksjonellException {
        AktoerDto aktoerDto = lagAktoerDto();
        aktoerDto.setDatabaseID(aktoerId);
        Fagsak fagsak = lagFagsak();
        Aktoer aktoerFromDatabase = new Aktoer();
        aktoerFromDatabase.setId(aktoerId);
        doReturn(Optional.of(aktoerFromDatabase)).when(aktørRepository).findById(aktoerDto.getDatabaseID());

        Long databaseId = aktørService.lagEllerOppdaterAktoer(fagsak, aktoerDto);

        ArgumentCaptor<Aktoer> captor = ArgumentCaptor.forClass(Aktoer.class);
        verify(aktørRepository).save(captor.capture());
        Aktoer aktoer = captor.getValue();

        assertAktoerData(aktoerDto, fagsak, aktoer);
        assertThat(aktoer.getId()).isEqualTo(aktoerId);
        assertThat(databaseId).isEqualTo(aktoerId);
    }

    @Test
    public final void hentfagsakAktoerer() {
        aktørService.hentfagsakAktører(lagFagsak(), Aktoersroller.REPRESENTANT, Representerer.BRUKER);

        verify(aktørRepository).findAll(exampleCaptor.capture());
        Example aktørExample = exampleCaptor.getValue();

        Aktoer aktørProbe = (Aktoer) aktørExample.getProbe();
        assertThat(aktørProbe.getFagsak()).isEqualTo(lagFagsak());
        assertThat(aktørProbe.getRolle()).isEqualTo(Aktoersroller.REPRESENTANT);
        assertThat(aktørProbe.getRepresenterer()).isEqualTo(Representerer.BRUKER);
    }

    @Test
    public void erstattEksisterendeArbeidsgiveraktører_medNyttOrgnr() {
        Fagsak fagsak = lagFagsak();
        List<String> orgnumre = Collections.singletonList("123456789");
        aktørService.erstattEksisterendeArbeidsgiveraktører(fagsak, orgnumre);
        verify(aktørRepository).deleteAllByFagsakAndRolle(eq(fagsak), eq(Aktoersroller.ARBEIDSGIVER));

        Aktoer aktoer = new Aktoer();
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(Aktoersroller.ARBEIDSGIVER);
        aktoer.setOrgnr("123456789");
        verify(aktørRepository).save(eq(aktoer));
    }

    @Test
    public void slettAktør_sletteBruker_kasterException() throws FunksjonellException, TekniskException {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        Optional<Aktoer> optionalAktoer = Optional.of(aktoer);
        doReturn(optionalAktoer).when(aktørRepository).findById(10L);
        expectedException.expect(FunksjonellException.class);

        aktørService.slettAktoer(10L);

        verify(aktørRepository, never()).deleteById(optionalAktoer.get());
    }

    @Test
    public void slettAktør_sletteRepresentant_fungerer() throws FunksjonellException, TekniskException {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.REPRESENTANT);
        Optional<Aktoer> optionalAktoer = Optional.of(aktoer);
        doReturn(optionalAktoer).when(aktørRepository).findById(10L);

        aktørService.slettAktoer(10L);

        verify(aktørRepository).deleteById(optionalAktoer.get());
    }

    @Test
    public void erstattEksisterendeArbeidsgiveraktører_utenNyeOrgnr() {
        Fagsak fagsak = lagFagsak();
        aktørService.erstattEksisterendeArbeidsgiveraktører(fagsak, Collections.emptyList());
        verify(aktørRepository).deleteAllByFagsakAndRolle(eq(fagsak), eq(Aktoersroller.ARBEIDSGIVER));
        verify(aktørRepository, never()).save(any());
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-1");
        return fagsak;
    }

    private void assertAktoerData(AktoerDto aktoerDto, Fagsak fagsak, Aktoer aktoer) {
        assertThat(aktoer.getFagsak()).isEqualTo(fagsak);
        assertThat(aktoer.getInstitusjonId()).isEqualTo(aktoerDto.getInstitusjonsID());
        assertThat(aktoer.getUtenlandskPersonId()).isEqualTo(aktoerDto.getUtenlandskPersonID());
        assertThat(aktoer.getOrgnr()).isEqualTo(aktoerDto.getOrgnr());
        assertThat(aktoer.getRolle().toString()).isEqualTo(aktoerDto.getRolleKode());
        assertThat(aktoer.getRepresenterer().toString()).isEqualTo(aktoerDto.getRepresentererKode());
    }

    private AktoerDto lagAktoerDto() {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setRolleKode("BRUKER");
        aktoerDto.setInstitusjonsID("institusjonsID");
        aktoerDto.setUtenlandskPersonID("utenlandskPersonID");
        aktoerDto.setOrgnr("orgnr");
        aktoerDto.setRepresentererKode("BRUKER");
        return aktoerDto;
    }
}
