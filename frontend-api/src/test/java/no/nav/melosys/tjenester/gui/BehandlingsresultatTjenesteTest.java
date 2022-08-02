package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.VedtakMetadata;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.begrunnelser.Nyvurderingbakgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.service.behandling.AngiBehandlingsresultatService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.BehandlingsresultatDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BehandlingsresultatTjenesteTest extends JsonSchemaTestParent {

    private BehandlingsresultatTjeneste behandlingsresultatTjeneste;

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private AngiBehandlingsresultatService angiBehandlingsresultatService;

    @BeforeEach
    public void setUp() {
        behandlingsresultatTjeneste = new BehandlingsresultatTjeneste(behandlingsresultatService, angiBehandlingsresultatService, mock(Aksesskontroll.class));
    }

    @Test
    void validerBehandlingsresultat() throws Exception {
        EasyRandomParameters easyRandomParameters = defaultEasyRandomParameters()
            .randomize(named("behandlingsresultatTypeKode").and(ofType(String.class)), () -> new EnumRandomizer<>(Behandlingsresultattyper.class).getRandomValue().getKode())
            .randomize(named("vedtakstype").and(ofType(String.class)), () -> new EnumRandomizer<>(Vedtakstyper.class, Vedtakstyper.ENDRINGSVEDTAK).getRandomValue().getKode());
        BehandlingsresultatDto behandlingsresultat = new EasyRandom(easyRandomParameters).nextObject(BehandlingsresultatDto.class);
        String jsonString = objectMapper().writeValueAsString(behandlingsresultat);
        assertThat(jsonString).isNotEmpty();
    }

    @Test
    void hentBehandlingsresultat_medBehandlingsid_forventerBehandlingsresultatDto() throws IOException {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.IKKE_FASTSATT);
        behandlingsresultat.setBegrunnelseFritekst("Bruker har fått flyskrekk");
        behandlingsresultat.setInnledningFritekst("<p>Bruker har fått flyskrekk</>");
        BehandlingsresultatBegrunnelse begrunnelse = new BehandlingsresultatBegrunnelse();
        begrunnelse.setKode(Henleggelsesgrunner.ANNET.getKode());
        behandlingsresultat.setBehandlingsresultatBegrunnelser(Sets.newHashSet(begrunnelse));
        VedtakMetadata vedtakMetadata = new VedtakMetadata();
        vedtakMetadata.setVedtakstype(Vedtakstyper.KORRIGERT_VEDTAK);
        vedtakMetadata.setNyVurderingBakgrunn(Nyvurderingbakgrunner.FEIL_I_BEHANDLING.getKode());
        behandlingsresultat.setVedtakMetadata(vedtakMetadata);
        when(behandlingsresultatService.hentBehandlingsresultatMedKontrollresultat(anyLong())).thenReturn(behandlingsresultat);

        ResponseEntity response = behandlingsresultatTjeneste.hentBehandlingsresultat(4L);
        String jsonString = objectMapper().writeValueAsString(response.getBody());
        assertThat(jsonString).isNotEmpty();
    }
}
