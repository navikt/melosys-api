package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.tjenester.gui.dto.periode.AnmodningsperiodeDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningsperiodeTjenesteTest extends JsonSchemaTestParent {
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private Tilgang tilgang;

    private AnmodningsperiodeTjeneste anmodningsperiodeTjeneste;

    private EasyRandom random = new EasyRandom(new EasyRandomParameters()
        .excludeField(ofType(Behandlingsresultat.class))
        .randomize(ofType(LovvalgBestemmelse.class), () -> new EnumRandomizer(LovvalgsBestemmelser_883_2004.class).getRandomValue()));

    @Before
    public void setUp() {
        anmodningsperiodeTjeneste = new AnmodningsperiodeTjeneste(anmodningsperiodeService, tilgang);
    }

    @Test
    public void hentAnmodningsperioder() throws Exception {
        Set<Anmodningsperiode> mockliste = random.objects(Anmodningsperiode.class, 3).collect(Collectors.toSet());
        when(anmodningsperiodeService.hentAnmodningsperioder(1L)).thenReturn(mockliste);

        Collection<AnmodningsperiodeDto> anmodningsperioder = anmodningsperiodeTjeneste.hentAnmodningsperioder(1L);
        assertThat(anmodningsperioder).isNotEmpty();
        //validerListe(anmodningsperioder); TODO schema
    }
}