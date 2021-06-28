package no.nav.melosys.tjenester.gui.graphql;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.graphql.dto.SaksopplysningerDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaksopplysningerDataFetcherTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private DataFetchingEnvironment dataFetchingEnvironment;

    @Test
    void get() throws Exception {
        SaksopplysningerDataFetcher saksopplysningerDataFetcher = new SaksopplysningerDataFetcher(
            behandlingService, kodeverkService, persondataFasade
        );
        final var statsborgerskap_1 = new Statsborgerskap("AAA", null, LocalDate.parse("2009-11-18"),
            LocalDate.parse("1980-11-18"), "PDL", "Dolly", false);
        final var statsborgerskap_2 = new Statsborgerskap("BBB", null, LocalDate.parse("1979-11-18"),
            LocalDate.parse("1980-11-18"), "PDL", "Dolly", false);
        final var statsborgerskap_3 = new Statsborgerskap("CCC", null, null, LocalDate.parse("1980-11-18"), "PDL",
            "Dolly", false);

        when(dataFetchingEnvironment.getArgument("behandlingID")).thenReturn(1L);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(lagBehandling());
        when(persondataFasade.hentStatsborgerskap(any())).thenReturn(Set.of(statsborgerskap_1, statsborgerskap_2,
            statsborgerskap_3)
        );
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "AAA")).thenReturn("Testland A");
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "BBB")).thenReturn("Testland B");
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "CCC")).thenReturn("Testland C");

        final var dataFetcherResult = saksopplysningerDataFetcher.get(dataFetchingEnvironment);
        Consumer<SaksopplysningerDto> statsborgerskapErSortert = saksopplysningerDto -> {
            assertThat(saksopplysningerDto.persondata().statsborgerskap().get(0).land()).isEqualTo("Testland C");
            assertThat(saksopplysningerDto.persondata().statsborgerskap().get(1).land()).isEqualTo("Testland A");
            assertThat(saksopplysningerDto.persondata().statsborgerskap().get(2).land()).isEqualTo("Testland B");
        };
        assertThat(dataFetcherResult.getData())
            .isInstanceOfSatisfying(SaksopplysningerDto.class, statsborgerskapErSortert);
    }

    private static Behandling lagBehandling() {
        final String aktørID = "123";
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        Aktoer aktør = new Aktoer();
        aktør.setAktørId(aktørID);
        aktør.setRolle(Aktoersroller.BRUKER);
        HashSet<Aktoer> aktører = new HashSet<>();
        aktører.add(aktør);
        fagsak.setAktører(aktører);
        behandling.setFagsak(fagsak);
        return behandling;
    }
}
