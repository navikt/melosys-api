package no.nav.melosys.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.AktoerRepository;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AktoerServiceTest {

    @Mock
    private AktoerRepository aktørRepository;

    private AktoerService aktørService;

    @Captor
    private ArgumentCaptor<Example> exampleCaptor;
    private long aktoerId = 234L;
    private Aktoer aktør;

    @BeforeEach
    public void setUp() {
        aktørService = new AktoerService(aktørRepository);
        aktør = new Aktoer();
        aktør.setId(aktoerId);
    }

    @Test
    void lagEllerOppdater_nyAktoer() {
        doReturn(aktør).when(aktørRepository).save(any());
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
    void lagEllerOppdater_oppdaterAktoer() {
        doReturn(aktør).when(aktørRepository).save(any());

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
    void hentfagsakAktoerer() {
        aktørService.hentfagsakAktører(lagFagsak(), Aktoersroller.REPRESENTANT, Representerer.BRUKER);

        verify(aktørRepository).findAll(exampleCaptor.capture());
        Example aktørExample = exampleCaptor.getValue();

        Aktoer aktørProbe = (Aktoer) aktørExample.getProbe();
        assertThat(aktørProbe.getFagsak()).isEqualTo(lagFagsak());
        assertThat(aktørProbe.getRolle()).isEqualTo(Aktoersroller.REPRESENTANT);
        assertThat(aktørProbe.getRepresenterer()).isEqualTo(Representerer.BRUKER);
    }

    @Test
    void erstattEksisterendeArbeidsgiveraktører_medNyttOrgnr() {
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
    void slettAktør_sletteBruker_kasterException() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        Optional<Aktoer> optionalAktoer = Optional.of(aktoer);
        doReturn(optionalAktoer).when(aktørRepository).findById(10L);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> aktørService.slettAktoer(10L))
            .withMessageContaining("er en bruker");

        verify(aktørRepository, never()).deleteByAktørId(optionalAktoer.get().getAktørId());
    }

    @Test
    void slettAktør_sletteRepresentant_fungerer() {
        Aktoer aktoer = new Aktoer();
        aktoer.setId(10L);
        aktoer.setRolle(Aktoersroller.REPRESENTANT);
        aktoer.setFagsak(new Fagsak());
        Optional<Aktoer> optionalAktoer = Optional.of(aktoer);
        doReturn(optionalAktoer).when(aktørRepository).findById(10L);

        aktørService.slettAktoer(10L);

        verify(aktørRepository).deleteById(optionalAktoer.get().getId());
    }

    @Test
    void erstattEksisterendeArbeidsgiveraktører_utenNyeOrgnr() {
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
