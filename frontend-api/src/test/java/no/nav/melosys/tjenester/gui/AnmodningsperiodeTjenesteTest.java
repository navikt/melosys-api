package no.nav.melosys.tjenester.gui;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeGetDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodePostDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSvarDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnmodningsperiodeTjenesteTest extends JsonSchemaTestParent {
    private static final Logger logger = LoggerFactory.getLogger(AnmodningsperiodeTjenesteTest.class);
    private static final String ANMODNINGSPERIODER_GET_SCHEMA = "anmodningsperioder-schema.json";
    private static final String ANMODNINGSPERIODER_POST_SCHEMA = "anmodningsperioder-post-schema.json";
    private static final String ANMODNINGSPERIODER_SVAR_SCHEMA = "anmodningsperioder-svar-schema.json";

    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private TilgangService tilgangService;

    private AnmodningsperiodeTjeneste anmodningsperiodeTjeneste;

    private EasyRandom random = new EasyRandom(new EasyRandomParameters()
        .excludeField(ofType(Behandlingsresultat.class))
        .randomize(ofType(LovvalgBestemmelse.class), () -> new EnumRandomizer<>(Lovvalgbestemmelser_883_2004.class).getRandomValue()));

    @BeforeEach
    public void setUp() {
        anmodningsperiodeTjeneste = new AnmodningsperiodeTjeneste(anmodningsperiodeService, lovvalgsperiodeService, tilgangService);
    }

    @Test
    public void hentAnmodningsperioder() throws Exception {
        when(anmodningsperiodeService.hentAnmodningsperioder(1L)).thenReturn(mockAnmodningsperioder());

        AnmodningsperiodeGetDto anmodningsperiodeGetDto = anmodningsperiodeTjeneste.hentAnmodningsperioder(1L);
        assertThat(anmodningsperiodeGetDto.getAnmodningsperioder()).isNotEmpty();
        valider(anmodningsperiodeGetDto, ANMODNINGSPERIODER_GET_SCHEMA, logger);
    }

    @Test
    public void lagreAnmodningsperioder() throws Exception {
        Set<Anmodningsperiode> mockAnmodninger = random.objects(Anmodningsperiode.class, 3).collect(Collectors.toSet());
        when(anmodningsperiodeService.lagreAnmodningsperioder(anyLong(), anyCollection()))
            .thenReturn(mockAnmodninger);

        AnmodningsperiodePostDto postDto = AnmodningsperiodePostDto.av(mockAnmodninger);

        AnmodningsperiodeGetDto anmodningsperiodeGetDto =
            anmodningsperiodeTjeneste.lagreAnmodningsperioder(1L, AnmodningsperiodePostDto.av(mockAnmodninger));

        verify(tilgangService).sjekkRedigerbarOgTilgang(anyLong());
        verify(anmodningsperiodeService).lagreAnmodningsperioder(anyLong(), anyCollection());
        valider(postDto, ANMODNINGSPERIODER_POST_SCHEMA, logger);
        valider(anmodningsperiodeGetDto, ANMODNINGSPERIODER_GET_SCHEMA, logger);
    }

    @Test
    public void hentAnmodningsperiodeSvar() throws Exception {

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        anmodningsperiode.setBehandlingsresultat(behandlingsresultat);
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setBegrunnelseFritekst("test");
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);

        when(anmodningsperiodeService.finnAnmodningsperiode(anyLong())).thenReturn(Optional.of(anmodningsperiode));

        AnmodningsperiodeSvarDto svarDto = anmodningsperiodeTjeneste.hentAnmodningsperiodeSvar(1L);
        assertThat(svarDto).isNotNull();
        assertThat(svarDto.begrunnelseFritekst).isNotEmpty();
        assertThat(svarDto.anmodningsperiodeSvarType).isEqualTo(Anmodningsperiodesvartyper.INNVILGELSE.name());
        valider(svarDto, ANMODNINGSPERIODER_SVAR_SCHEMA, logger);
    }

    @Test
    public void lagreAnmodningsperiodeSvar() throws Exception {

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        anmodningsperiode.setBehandlingsresultat(behandlingsresultat);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        svar.setBegrunnelseFritekst("fritekst");
        svar.setAnmodningsperiode(anmodningsperiode);
        anmodningsperiode.setAnmodningsperiodeSvar(svar);

        when(anmodningsperiodeService.finnAnmodningsperiode(anyLong())).thenReturn(Optional.of(anmodningsperiode));
        when(anmodningsperiodeService.lagreAnmodningsperiodeSvar(anyLong(), any()))
            .thenReturn(svar);

        AnmodningsperiodeSvarDto svarDto = anmodningsperiodeTjeneste.lagreAnmodningsperiodeSvar(1L, new AnmodningsperiodeSvarDto());
        assertThat(svarDto).isNotNull();
        assertThat(svarDto.anmodningsperiodeSvarType).isEqualTo(Anmodningsperiodesvartyper.INNVILGELSE.name());
        verify(tilgangService).sjekkRedigerbarOgTilgang(anyLong());
        valider(svarDto, ANMODNINGSPERIODER_SVAR_SCHEMA, logger);
    }

    private Set<Anmodningsperiode> mockAnmodningsperioder() {
        return random.objects(Anmodningsperiode.class, 3).collect(Collectors.toSet());
    }
}
