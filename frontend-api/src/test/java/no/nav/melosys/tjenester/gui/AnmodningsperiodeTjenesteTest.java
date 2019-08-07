package no.nav.melosys.tjenester.gui;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.AnmodningsperiodeSvarType;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeListeDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSvarDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningsperiodeTjenesteTest extends JsonSchemaTestParent {
    private static final Logger logger = LoggerFactory.getLogger(AnmodningsperiodeTjenesteTest.class);
    private static final String ANMODNINGSPERIODER_GET_SCHEMA = "anmodningsperioder-get-schema.json";
    private static final String ANMODNINGSPERIODER_POST_SCHEMA = "anmodningsperioder-post-schema.json";
    private static final String ANMODNINGSPERIODER_SVAR_SCHEMA = "anmodningsperiodersvar-schema.json";

    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private TilgangService tilgangService;

    private AnmodningsperiodeTjeneste anmodningsperiodeTjeneste;

    private EasyRandom random = new EasyRandom(new EasyRandomParameters()
        .excludeField(ofType(Behandlingsresultat.class))
        .randomize(ofType(LovvalgBestemmelse.class), () -> new EnumRandomizer<>(LovvalgsBestemmelser_883_2004.class).getRandomValue()));

    @Before
    public void setUp() {
        anmodningsperiodeTjeneste = new AnmodningsperiodeTjeneste(anmodningsperiodeService, tilgangService);
    }

    @Test
    public void hentAnmodningsperioder() throws Exception {
        when(anmodningsperiodeService.hentAnmodningsperioder(1L)).thenReturn(mockAnmodningsperioder());

        AnmodningsperiodeListeDto anmodningsperiodeListeDto = anmodningsperiodeTjeneste.hentAnmodningsperioder(1L);
        assertThat(anmodningsperiodeListeDto.getAnmodningsperioder()).isNotEmpty();
        valider(ANMODNINGSPERIODER_GET_SCHEMA, anmodningsperiodeListeDto, logger);
    }

    @Test
    public void lagreAnmodningsperioder() throws Exception {
        Set<Anmodningsperiode> mockAnmodninger = random.objects(Anmodningsperiode.class, 3).collect(Collectors.toSet());
        when(anmodningsperiodeService.lagreAnmodningsperioder(anyLong(), anyCollection()))
            .thenReturn(mockAnmodninger);

        AnmodningsperiodeListeDto anmodningsperiodeListeDto =
            anmodningsperiodeTjeneste.lagreAnmodningsperioder(1L, AnmodningsperiodeListeDto.av(mockAnmodninger));

        verify(tilgangService).sjekkRedigerbarOgTilgang(anyLong());
        verify(anmodningsperiodeService).lagreAnmodningsperioder(anyLong(), anyCollection());
        valider(ANMODNINGSPERIODER_POST_SCHEMA, anmodningsperiodeListeDto, logger);
    }

    @Test
    public void hentAnmodningsperiodeSvar() throws Exception {

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        anmodningsperiode.setBehandlingsresultat(behandlingsresultat);
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setBegrunnelseFritekst("test");
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.INNVILGELSE);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);

        when(anmodningsperiodeService.hentAnmodningsperiode(anyLong())).thenReturn(Optional.of(anmodningsperiode));

        AnmodningsperiodeSvarDto svarDto = anmodningsperiodeTjeneste.hentAnmodningsperiodeSvar(1L);
        assertThat(svarDto).isNotNull();
        assertThat(svarDto.begrunnelseFritekst).isNotEmpty();
        assertThat(svarDto.anmodningsperiodeSvarType).isEqualTo(AnmodningsperiodeSvarType.INNVILGELSE.name());
        valider(ANMODNINGSPERIODER_SVAR_SCHEMA, svarDto, logger);
    }

    @Test
    public void lagreAnmodningsperiodeSvar() throws Exception {

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        anmodningsperiode.setBehandlingsresultat(behandlingsresultat);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.INNVILGELSE);
        svar.setBegrunnelseFritekst("fritekst");

        when(anmodningsperiodeService.hentAnmodningsperiode(anyLong())).thenReturn(Optional.of(anmodningsperiode));
        when(anmodningsperiodeService.lagreAnmodningsperiodeSvar(anyLong(), any()))
            .thenReturn(svar);

        AnmodningsperiodeSvarDto svarDto = anmodningsperiodeTjeneste.lagreAnmodningsperiodeSvar(1L, new AnmodningsperiodeSvarDto());
        assertThat(svarDto).isNotNull();
        assertThat(svarDto.anmodningsperiodeSvarType).isEqualTo(AnmodningsperiodeSvarType.INNVILGELSE.name());
        verify(tilgangService).sjekkRedigerbarOgTilgang(anyLong());
        valider(ANMODNINGSPERIODER_SVAR_SCHEMA, svarDto, logger);
    }

    private Set<Anmodningsperiode> mockAnmodningsperioder() {
        return random.objects(Anmodningsperiode.class, 3).collect(Collectors.toSet());
    }
}