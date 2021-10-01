package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingsperioderDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtpekingsperiodeTjenesteTest extends JsonSchemaTestParent {

    private static final Logger log = LoggerFactory.getLogger(UtpekingsperiodeTjenesteTest.class);
    private static final String UTPEKINGSPERIODER_SCHEMA = "utpekingsperioder-schema.json";
    private EasyRandom random = new EasyRandom(new EasyRandomParameters()
        .randomize(ofType(LovvalgBestemmelse.class), () ->
            new EnumRandomizer<>(Lovvalgbestemmelser_883_2004.class).getRandomValue()));
    @Captor
    ArgumentCaptor<List<Utpekingsperiode>> captor;

    @Mock
    private Aksesskontroll aksesskontroll;
    @Mock
    private UtpekingService utpekingService;
    private UtpekingsperiodeTjeneste utpekingsperiodeTjeneste;

    @BeforeEach
    public void settOpp() {
        utpekingsperiodeTjeneste = new UtpekingsperiodeTjeneste(utpekingService, aksesskontroll);
    }

    @Test
    void hentUtpekingsperioder() throws IOException {
        when(utpekingService.hentUtpekingsperioder(anyLong())).thenReturn(lagUtpekingsperioder());

        UtpekingsperioderDto utpekingsperioderDto = utpekingsperiodeTjeneste.hentUtpekingsperioder(123L);

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(utpekingsperioderDto);
        valider(jsonString, UTPEKINGSPERIODER_SCHEMA, log);

        verify(aksesskontroll).autoriser(anyLong());
        verify(utpekingService).hentUtpekingsperioder(anyLong());
    }

    @Test
    void deserialiserUtpekingsperioder() throws Exception {
        String json = """
            {
               "utpekingsperioder":[
                  {
                     "fomDato":"2021-08-13",
                     "tomDato":"2021-11-11",
                     "lovvalgsbestemmelse":"FO_883_2004_ART13_1B1",
                     "tilleggsbestemmelse":null,
                     "lovvalgsland":"FR"
                  }
               ]
            }""";

        assertThat(objectMapper().readValue(json, UtpekingsperioderDto.class))
            .extracting(UtpekingsperioderDto::utpekingsperioder)
            .asList()
            .hasSize(1);
    }

    @Test
    void lagreUtpekingsperioder() throws IOException {
        UtpekingsperioderDto utpekingsperioderDto = UtpekingsperioderDto.av(lagUtpekingsperioder());

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(utpekingsperioderDto);
        valider(jsonString, UTPEKINGSPERIODER_SCHEMA, log);

        utpekingsperiodeTjeneste.lagreUtpekingsperioder(123L, utpekingsperioderDto);

        verify(aksesskontroll).autoriserSkriv(anyLong());
        verify(utpekingService).lagreUtpekingsperioder(eq(123L), captor.capture());

        List<Utpekingsperiode> utpekingsperioder = captor.getValue();
        assertThat(utpekingsperioder.size()).isEqualTo(3);
    }

    private List<Utpekingsperiode> lagUtpekingsperioder() {
        Utpekingsperiode utpekingsperiodeUtenTilleggsbestemmelse = new Utpekingsperiode(
            LocalDate.now(),
            LocalDate.now(),
            Landkoder.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
            null
        );

        return Arrays.asList(
            utpekingsperiodeUtenTilleggsbestemmelse,
            random.nextObject(Utpekingsperiode.class),
            random.nextObject(Utpekingsperiode.class)
        );
    }
}
