package no.nav.melosys.tjenester.gui;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.AnmodningsperiodeSvarType;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.tjenester.gui.dto.periode.AnmodningsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.AnmodningsperiodeSvarDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
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

    @Test
    public void lagreAnmodningsperioder() throws Exception {
        when(anmodningsperiodeService.lagreAnmodningsperioder(anyLong(), anyCollection())).thenReturn(Collections.singletonList(hentAnmodningsperiodeMedId()));

        List<AnmodningsperiodeDto> anmodningsperiodeDtoer = new ArrayList<>();
        anmodningsperiodeDtoer.add(AnmodningsperiodeDto.av(hentAnmodningsperiodeMedId()));

        anmodningsperiodeTjeneste.lagreAnmodningsperioder(1L, anmodningsperiodeDtoer);

        verify(tilgang).sjekk(anyLong());
        verify(anmodningsperiodeService).lagreAnmodningsperioder(anyLong(), anyCollection());
        //TODO schema
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
        //TODO schema
    }

    @Test
    public void lagreAnmodningsperiodeSvar() throws Exception {

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        anmodningsperiode.setBehandlingsresultat(behandlingsresultat);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.INNVILGELSE);

        when(anmodningsperiodeService.hentAnmodningsperiode(anyLong())).thenReturn(Optional.of(anmodningsperiode));
        when(anmodningsperiodeService.lagreAnmodningsperiodeSvar(anyLong(), any()))
            .thenReturn(svar);

        AnmodningsperiodeSvarDto svarDto = anmodningsperiodeTjeneste.lagreAnmodningsperiodeSvar(1L, new AnmodningsperiodeSvarDto());
        assertThat(svarDto).isNotNull();
        assertThat(svarDto.anmodningsperiodeSvarType).isEqualTo(AnmodningsperiodeSvarType.INNVILGELSE.name());
        //TODO schema
    }

    private Anmodningsperiode hentAnmodningsperiodeMedId() {

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        ReflectionTestUtils.setField(anmodningsperiode, "id", 1L);
        return anmodningsperiode;
    }
}