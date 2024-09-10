package no.nav.melosys.service.vedtak;

import java.util.stream.Stream;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.util.Assert;

class FattVedtakVelgerTest {
    FattVedtakVelger fattVedtakVelger = new FattVedtakVelger(
        Mockito.mock(EosVedtakService.class),
        Mockito.mock(FtrlVedtakService.class),
        Mockito.mock(TrygdeavtaleVedtakService.class),
        Mockito.mock(ÅrsavregningVedtakService.class)
    );

    @ParameterizedTest
    @MethodSource("sakOgBehandlingKombinasjoner")
    void getFattVedtakServiceVelgerRiktigImplementasjon(Sakstyper sakstyper, Boolean erÅrsavregningBehandling, Class<FattVedtakInterface> fattVedtakServiceImplementasjon) {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setType(sakstyper);
        behandling.setFagsak(fagsak);

        if (erÅrsavregningBehandling) {
            behandling.setType(Behandlingstyper.ÅRSAVREGNING);
        }


        FattVedtakInterface fattVedtakService = fattVedtakVelger.getFattVedtakService(behandling);


        var erForventetImplementasjon = fattVedtakService.getClass().equals(fattVedtakServiceImplementasjon);
        Assert.isTrue(erForventetImplementasjon, "implementasjon skal være " + fattVedtakServiceImplementasjon);
    }

    private static Stream<Arguments> sakOgBehandlingKombinasjoner() {
        return Stream.of(
            Arguments.of(Sakstyper.FTRL, false, FtrlVedtakService.class),
            Arguments.of(Sakstyper.FTRL, true, ÅrsavregningVedtakService.class),
            Arguments.of(Sakstyper.EU_EOS, false, EosVedtakService.class),
            Arguments.of(Sakstyper.EU_EOS, true, ÅrsavregningVedtakService.class),
            Arguments.of(Sakstyper.TRYGDEAVTALE, false, TrygdeavtaleVedtakService.class),
            Arguments.of(Sakstyper.TRYGDEAVTALE, true, ÅrsavregningVedtakService.class)
        );
    }
}
