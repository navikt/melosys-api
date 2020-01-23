package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingsperioderDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UtpekingsperiodeTjenesteTest extends JsonSchemaTestParent {

    private static final Logger log = LoggerFactory.getLogger(UtpekingsperiodeTjenesteTest.class);
    private static final String UTPEKINGSPERIODER_SCHEMA = "utpekingsperioder-schema.json";
    private EasyRandom random = new EasyRandom(new EasyRandomParameters()
        .collectionSizeRange(2, 4)
        .randomize(named("lovvalgsland"), () -> "SE")
        .randomize(named("lovvalgsbestemmelse"), () -> "FO_883_2004_ART11_1")
        .randomize(named("tilleggsbestemmelse"), () -> "FO_883_2004_ART11_1")
        .randomize(ofType(LovvalgBestemmelse.class), () -> new EnumRandomizer<>(Lovvalgbestemmelser_883_2004.class).getRandomValue()));

    @Mock
    private TilgangService tilgangService;
    @Mock
    private UtpekingService utpekingService;
    private UtpekingsperiodeTjeneste utpekingsperiodeTjeneste;

    /* FIXME
    @Before
    public void settOpp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        utpekingsperiodeTjeneste = new UtpekingsperiodeTjeneste(tilgangService, utpekingService);

        List<Utpekingsperiode> utpekingsperioder = Arrays.asList(
            random.nextObject(Utpekingsperiode.class),
            random.nextObject(Utpekingsperiode.class)
        );

        when(utpekingService.hentUtpekingsperioder(anyLong())).thenReturn(utpekingsperioder);
    }
    */

    @Ignore // FIXME
    @Test
    public void hentUtpekingsperioder() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        UtpekingsperioderDto utpekingsperioderDto = utpekingsperiodeTjeneste.hentUtpekingsperioder(123L);

        assertThat(utpekingsperioderDto.getUtpekingsperioder().size()).isEqualTo(2);

        verify(tilgangService).sjekkTilgang(anyLong());
        verify(utpekingService).hentUtpekingsperioder(anyLong());
    }

    @Ignore // FIXME
    @Test
    public void postUtpekingsperioder() throws IOException {
        // FIXME - samme skjema for GET og POST - test endepunktene med validert JSON
        UtpekingsperioderDto utpekingsperioderDto = random.nextObject(UtpekingsperioderDto.class);

        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(utpekingsperioderDto);
        valider(jsonString, UTPEKINGSPERIODER_SCHEMA, log);
    }

    @Ignore // FIXME
    @Test
    public void lagreUtpekingsperioder() throws FunksjonellException, TekniskException {
        UtpekingsperioderDto utpekingsperioderDto = random.nextObject(UtpekingsperioderDto.class);
        utpekingsperioderDto.getUtpekingsperioder().forEach(
            utpekingsperiodeDto -> utpekingsperiodeDto.setLovvalgsland("SE")
        );

        utpekingsperiodeTjeneste.lagreUtpekingsperioder(123L, utpekingsperioderDto);

        verify(tilgangService).sjekkRedigerbarOgTilgang(anyLong());
        verify(utpekingService).lagreUtpekingsperioder(anyLong(), anyCollection());
    }
}
