package no.nav.melosys.tjenester.gui;


import java.util.List;
import java.util.UUID;

import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;
import no.nav.melosys.domain.kodeverk.Flyvningstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagGetDto;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BehandlingsgrunnlagTjenesteTest {

    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private Aksesskontroll aksesskontroll;

    private BehandlingsgrunnlagTjeneste behandlingsgrunnlagTjeneste;

    private EasyRandom random;

    @BeforeEach
    public void setup() {
        behandlingsgrunnlagTjeneste = new BehandlingsgrunnlagTjeneste(behandlingsgrunnlagService, aksesskontroll);

        random = new EasyRandom(new EasyRandomParameters()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .randomize(GeografiskAdresse.class, () -> new EasyRandom().nextObject(SemistrukturertAdresse.class))
            .stringLengthRange(2, 10)
            .randomize(named("fnr").and(ofType(String.class)), new NumericStringRandomizer(11))
            .randomize(named("orgnr").and(ofType(String.class)), new NumericStringRandomizer(9))
            .randomize(named("orgnummer").and(ofType(String.class)), new NumericStringRandomizer(9))
            .randomize(named("typeFlyvninger"), () -> new EnumRandomizer<>(Flyvningstyper.class).getRandomValue())
            .randomize(named("uuid"), () -> UUID.randomUUID().toString()));
    }

    @Test
    void hentBehandlingsgrunnlag_erSoeknad_validerSchema() {
        Soeknad soeknad = random.nextObject(Soeknad.class);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(soeknad);
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(anyLong())).thenReturn(behandlingsgrunnlag);

        ResponseEntity<BehandlingsgrunnlagGetDto> responseEntity = behandlingsgrunnlagTjeneste.hentBehandlingsgrunnlag(123);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isInstanceOf(BehandlingsgrunnlagGetDto.class);
    }

    @Test
    void hentBehandlingsgrunnlag_erSedGrunnlag_validerSchema()  {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(Behandlingsgrunnlagtyper.SED);

        SedGrunnlag sedGrunnlag = random.nextObject(SedGrunnlag.class);
        sedGrunnlag.overgangsregelbestemmelser = List.of(Overgangsregelbestemmelser.FO_1408_1971_ART14_2_A, Overgangsregelbestemmelser.FO_1408_1971_ART14_2_B);
        sedGrunnlag.ytterligereInformasjon = "fritekst";
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(sedGrunnlag);

        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(anyLong())).thenReturn(behandlingsgrunnlag);

        ResponseEntity<BehandlingsgrunnlagGetDto> responseEntity = behandlingsgrunnlagTjeneste.hentBehandlingsgrunnlag(1);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).isInstanceOf(BehandlingsgrunnlagGetDto.class);
    }
}
