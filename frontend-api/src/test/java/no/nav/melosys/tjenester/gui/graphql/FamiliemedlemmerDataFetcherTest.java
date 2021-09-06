package no.nav.melosys.tjenester.gui.graphql;

import java.time.LocalDate;
import java.util.Set;

import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.domain.person.familie.Familierelasjon;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.graphql.dto.FamiliemedlemDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamiliemedlemmerDataFetcherTest {
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private DataFetchingEnvironment dataFetchingEnvironment;
    @Mock
    private ExecutionStepInfo executionStepInfo;

    @Test
    void get() throws Exception {
        FamiliemedlemmerDataFetcher familiemedlemmerDataFetcher = new FamiliemedlemmerDataFetcher(persondataFasade);
        Set<Familiemedlem> medlemmer = Set.of(lagBarn(), lagRelatertVedsivilstand());
        when(persondataFasade.hentFamiliemedlemmerMedHistorikk(anyLong())).thenReturn(medlemmer);
        when(dataFetchingEnvironment.getExecutionStepInfo()).thenReturn(executionStepInfo);
        when(executionStepInfo.getParent()).thenReturn(executionStepInfo);
        when(executionStepInfo.getArgument("behandlingID")).thenReturn(1L);

        final var familieDtoListe = familiemedlemmerDataFetcher.get(dataFetchingEnvironment);
        assertThat(familieDtoListe).containsExactlyInAnyOrder(
            new FamiliemedlemDto("etternavn barn", "fnrBarn", Familierelasjon.BARN, 42, "felles", "fnrAnnenForelder",
                null, null),
            new FamiliemedlemDto("etternavn fornavn", "fnr", Familierelasjon.RELATERT_VED_SIVILSTAND, null, null, null,
                Sivilstandstype.GIFT, LocalDate.MIN));
    }

    private Familiemedlem lagBarn() {
        return new Familiemedlem(new Folkeregisteridentifikator("fnrBarn"), new Navn("barn", null, "etternavn"),
            Familierelasjon.BARN, new Foedsel(LocalDate.now().minusYears(42), null, null, null),
            new Folkeregisteridentifikator("fnrAnnenForelder"), "felles", null);
    }

    private Familiemedlem lagRelatertVedsivilstand() {
        return new Familiemedlem(new Folkeregisteridentifikator("fnr"), new Navn("fornavn", null, "etternavn"),
            Familierelasjon.RELATERT_VED_SIVILSTAND, new Foedsel(LocalDate.MIN, null, null, null), null, "ukjent",
            new Sivilstand(Sivilstandstype.GIFT, "relatertVedSivilstandID", LocalDate.MIN, null, "Dolly", "PDL",
                false));
    }
}
