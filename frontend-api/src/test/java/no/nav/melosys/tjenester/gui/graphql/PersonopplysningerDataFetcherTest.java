package no.nav.melosys.tjenester.gui.graphql;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Consumer;

import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersonMedHistorikk;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.graphql.dto.BostedsadresseDto;
import no.nav.melosys.tjenester.gui.graphql.dto.OppholdsadresseDto;
import no.nav.melosys.tjenester.gui.graphql.dto.PersonopplysningerDto;
import no.nav.melosys.tjenester.gui.graphql.dto.StrukturertAdresseformatDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonopplysningerDataFetcherTest {
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private DataFetchingEnvironment dataFetchingEnvironment;
    @Mock
    private ExecutionStepInfo executionStepInfo;

    @Test
    void get() throws Exception {
        PersonopplysningerDataFetcher personopplysningerDataFetcher = new PersonopplysningerDataFetcher(kodeverkService,
            persondataFasade);
        final var bostedsadresse_1 = new Bostedsadresse(
            new StrukturertAdresse("gate1", null, null, null, null, null),
            null, null, null, null, null, false);
        final var bostedsadresse_2 = new Bostedsadresse(
            new StrukturertAdresse("gate2", null, null, null, null, null),
            null, null, null, null, null, true);

        final var kontaktadresse_1 = new Kontaktadresse(
            new StrukturertAdresse("kontakt 1", null, null, null, null, null), null, null, null, null, null, null, null,
            false);
        final var kontaktadresse_2 = new Kontaktadresse(null,
            new SemistrukturertAdresse("kontakt 2", "linje 2", null, null, "1234", "By", "IT"), null, null, null, null,
            null, null, false);

        final var oppholdsadresse_1 = new Oppholdsadresse(
            new StrukturertAdresse("opphold 1", null, null, null, null, null), null, null, null, null, null, null,
            false);
        final var oppholdsadresse_2 = new Oppholdsadresse(
            new StrukturertAdresse("opphold 2", null, null, null, null, null), null, null, null, null, null, null,
            true);

        final var statsborgerskap_1 = new Statsborgerskap("AAA", null, LocalDate.parse("2009-11-18"),
            LocalDate.parse("1980-11-18"), "PDL", "Dolly", false);
        final var statsborgerskap_2 = new Statsborgerskap("BBB", null, LocalDate.parse("1979-11-18"),
            LocalDate.parse("1980-11-18"), "PDL", "Dolly", false);
        final var statsborgerskap_3 = new Statsborgerskap("CCC", null, null, LocalDate.parse("1980-11-18"), "PDL",
            "Dolly", false);

        when(dataFetchingEnvironment.getExecutionStepInfo()).thenReturn(executionStepInfo);
        when(executionStepInfo.getParent()).thenReturn(executionStepInfo);
        when(executionStepInfo.getArgument("behandlingID")).thenReturn(1L);
        when(persondataFasade.hentPersonMedHistorikk(anyLong())).thenReturn(
            new PersonMedHistorikk(Set.of(bostedsadresse_1, bostedsadresse_2), null, null, null, null,
                Set.of(kontaktadresse_1, kontaktadresse_2), null, Set.of(oppholdsadresse_1, oppholdsadresse_2),
                Set.of(statsborgerskap_1, statsborgerskap_2, statsborgerskap_3))
        );
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "AAA")).thenReturn("Testland A");
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "BBB")).thenReturn("Testland B");
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "CCC")).thenReturn("Testland C");

        final var dataFetcherResult = personopplysningerDataFetcher.get(dataFetchingEnvironment);
        assertThat(dataFetcherResult.bostedsadresser()).extracting(BostedsadresseDto::adresse)
            .extracting(StrukturertAdresseformatDto::gatenavn).containsExactlyInAnyOrder("gate1", "gate2");
        assertThat(dataFetcherResult.kontaktadresser()).hasSize(2);
        assertThat(dataFetcherResult.oppholdsadresser()).extracting(OppholdsadresseDto::adresse)
            .extracting(StrukturertAdresseformatDto::gatenavn).containsExactlyInAnyOrder("opphold 1", "opphold 2");

        Consumer<PersonopplysningerDto> statsborgerskapErSortert = personopplysningerDto -> {
            assertThat(personopplysningerDto.statsborgerskap().get(0).land()).isEqualTo("Testland C");
            assertThat(personopplysningerDto.statsborgerskap().get(1).land()).isEqualTo("Testland A");
            assertThat(personopplysningerDto.statsborgerskap().get(2).land()).isEqualTo("Testland B");
        };
        assertThat(dataFetcherResult).isInstanceOfSatisfying(PersonopplysningerDto.class, statsborgerskapErSortert);
    }
}
