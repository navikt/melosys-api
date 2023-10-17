package no.nav.melosys.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.AktoerRepository;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.persondata.PersondataFasade;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AktoerServiceTest {

    @Mock
    private AktoerRepository aktoerRepository;

    private AktoerService aktoerService;

    @Captor
    private ArgumentCaptor<Example<Aktoer>> exampleCaptor;
    @Captor
    private ArgumentCaptor<Aktoer> aktoerCaptor;
    private final long aktoerId = 234L;
    private Aktoer aktoer;

    @BeforeEach
    public void setUp() {
        aktoerService = new AktoerService(aktoerRepository);
        aktoer = new Aktoer();
        aktoer.setId(aktoerId);
    }

    @Test
    void lagEllerOppdater_nyAktoer() {
        AktoerDto aktoerDto = lagAktoerDto();
        Fagsak fagsak = lagFagsak();
        doReturn(aktoer).when(aktoerRepository).save(any());


        Long databaseId = aktoerService.lagEllerOppdaterAktoer(fagsak, aktoerDto);


        verify(aktoerRepository).save(aktoerCaptor.capture());
        Aktoer aktoer = aktoerCaptor.getValue();
        assertAktoerData(aktoerDto, fagsak, aktoer);
        assertThat(aktoer.getId()).isNull();
        assertThat(databaseId).isEqualTo(aktoerId);
    }

    @Test
    void lagEllerOppdater_oppdaterAktoer() {
        AktoerDto aktoerDto = lagAktoerDto();
        aktoerDto.setDatabaseID(aktoerId);
        Fagsak fagsak = lagFagsak();
        Aktoer aktoerFromDatabase = new Aktoer();
        aktoerFromDatabase.setId(aktoerId);

        doReturn(aktoer).when(aktoerRepository).save(any());
        doReturn(Optional.of(aktoerFromDatabase)).when(aktoerRepository).findById(aktoerDto.getDatabaseID());


        Long databaseId = aktoerService.lagEllerOppdaterAktoer(fagsak, aktoerDto);


        verify(aktoerRepository).save(aktoerCaptor.capture());
        Aktoer aktoer = aktoerCaptor.getValue();
        assertAktoerData(aktoerDto, fagsak, aktoer);
        assertThat(aktoer.getId()).isEqualTo(aktoerId);
        assertThat(databaseId).isEqualTo(aktoerId);
    }

    @Test
    void hentfagsakAktoerer() {
        Fagsak fagsak = lagFagsak();


        aktoerService.hentfagsakAktører(fagsak, Aktoersroller.REPRESENTANT, Representerer.BRUKER);


        verify(aktoerRepository).findAll(exampleCaptor.capture());
        Example<Aktoer> aktoerExample = exampleCaptor.getValue();

        Aktoer aktoerProbe = aktoerExample.getProbe();
        assertThat(aktoerProbe.getFagsak()).isEqualTo(lagFagsak());
        assertThat(aktoerProbe.getRolle()).isEqualTo(Aktoersroller.REPRESENTANT);
        assertThat(aktoerProbe.getRepresenterer()).isEqualTo(Representerer.BRUKER);
    }

    @Test
    void erstattEksisterendeArbeidsgiveraktører_medNyttOrgnr() {
        Fagsak fagsak = lagFagsak();
        List<String> orgnumre = Collections.singletonList("123456789");


        aktoerService.erstattEksisterendeArbeidsgiveraktører(fagsak, orgnumre);


        verify(aktoerRepository).deleteAllByFagsakAndRolle(fagsak, Aktoersroller.ARBEIDSGIVER);
        Aktoer aktoer = new Aktoer();
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(Aktoersroller.ARBEIDSGIVER);
        aktoer.setOrgnr("123456789");
        verify(aktoerRepository).save(aktoer);
    }

    @Test
    void slettAktør_sletteBruker_kasterException() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        Optional<Aktoer> optionalAktoer = Optional.of(aktoer);
        doReturn(optionalAktoer).when(aktoerRepository).findById(10L);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> aktoerService.slettAktoer(10L))
            .withMessageContaining("er en bruker");

        verify(aktoerRepository, never()).deleteByAktørId(optionalAktoer.get().getAktørId());
    }

    @Test
    void slettAktør_sletteRepresentant_fungerer() {
        Aktoer aktoer = new Aktoer();
        aktoer.setId(10L);
        aktoer.setRolle(Aktoersroller.REPRESENTANT);
        aktoer.setFagsak(new Fagsak());
        Optional<Aktoer> optionalAktoer = Optional.of(aktoer);
        doReturn(optionalAktoer).when(aktoerRepository).findById(10L);


        aktoerService.slettAktoer(10L);


        verify(aktoerRepository).deleteById(optionalAktoer.get().getId());
    }

    @Test
    void erstattEksisterendeArbeidsgiveraktører_utenNyeOrgnr() {
        Fagsak fagsak = lagFagsak();


        aktoerService.erstattEksisterendeArbeidsgiveraktører(fagsak, Collections.emptyList());


        verify(aktoerRepository).deleteAllByFagsakAndRolle(fagsak, Aktoersroller.ARBEIDSGIVER);
        verify(aktoerRepository, never()).save(any());
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-1");
        return fagsak;
    }

    private void assertAktoerData(AktoerDto aktoerDto, Fagsak fagsak, Aktoer aktoer) {
        assertThat(aktoer.getFagsak()).isEqualTo(fagsak);
        assertThat(aktoer.getInstitusjonId()).isEqualTo(aktoerDto.getInstitusjonsID());
        assertThat(aktoer.getUtenlandskPersonId()).isEqualTo(aktoerDto.getUtenlandskPersonID());
        assertThat(aktoer.getOrgnr()).isEqualTo(aktoerDto.getOrgnr());
        assertThat(aktoer.getRolle()).hasToString(aktoerDto.getRolleKode());
        assertThat(aktoer.getRepresenterer()).hasToString(aktoerDto.getRepresentererKode());
        assertThat(aktoer.getFullmaktstyper()).isEqualTo(aktoerDto.getFullmakter());
        assertThat(aktoer.getPersonIdent()).isEqualTo(aktoerDto.getPersonIdent());
    }

    private AktoerDto lagAktoerDto() {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setRolleKode("BRUKER");
        aktoerDto.setInstitusjonsID("institusjonsID");
        aktoerDto.setUtenlandskPersonID("utenlandskPersonID");
        aktoerDto.setOrgnr("orgnr");
        aktoerDto.setRepresentererKode("BRUKER");
        aktoerDto.setPersonIdent("21075114491");
        aktoerDto.setFullmakter(Set.of(Fullmaktstype.FULLMEKTIG_SØKNAD));
        return aktoerDto;
    }
}
