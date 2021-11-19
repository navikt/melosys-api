package no.nav.melosys.tjenester.gui.graphql;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Consumer;

import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Personstatuser;
import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersonMedHistorikk;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.graphql.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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
    void get_medBehandlingID_returnerData() throws Exception {
        PersonopplysningerDataFetcher personopplysningerDataFetcher = new PersonopplysningerDataFetcher(kodeverkService,
            persondataFasade);

        when(dataFetchingEnvironment.getExecutionStepInfo()).thenReturn(executionStepInfo);
        when(executionStepInfo.getParent()).thenReturn(executionStepInfo);
        when(executionStepInfo.getArgument("behandlingID")).thenReturn(1L);
        when(persondataFasade.hentPersonMedHistorikk(anyLong())).thenReturn(lagPersonMedHistorikk());
        when(kodeverkService.dekod(eq(FellesKodeverk.LANDKODER_ISO2), any())).thenReturn("My country");
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "AAA")).thenReturn("Testland A");
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "BBB")).thenReturn("Testland B");
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "CCC")).thenReturn("Testland C");

        final var personopplysninger = personopplysningerDataFetcher.get(dataFetchingEnvironment);

        assertFetched(personopplysninger);
    }

    @Test
    void get_medIdent_returnerData() throws Exception {
        PersonopplysningerDataFetcher personopplysningerDataFetcher = new PersonopplysningerDataFetcher(kodeverkService,
            persondataFasade);

        when(dataFetchingEnvironment.getArgument("ident")).thenReturn("Z990077");
        when(persondataFasade.hentPersonMedHistorikk("Z990077")).thenReturn(lagPersonMedHistorikk());
        when(kodeverkService.dekod(eq(FellesKodeverk.LANDKODER_ISO2), any())).thenReturn("My country");
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "AAA")).thenReturn("Testland A");
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "BBB")).thenReturn("Testland B");
        when(kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, "CCC")).thenReturn("Testland C");

        final var personopplysninger = personopplysningerDataFetcher.get(dataFetchingEnvironment);

        assertFetched(personopplysninger);
    }

    private void assertFetched(PersonopplysningerDto personopplysninger) {
        assertThat(personopplysninger.bostedsadresser()).extracting(BostedsadresseDto::adresse)
            .extracting(StrukturertAdresseformatDto::gatenavn).containsExactlyInAnyOrder("gate1", "gate2");
        assertThat(personopplysninger.bostedsadresser()).extracting(BostedsadresseDto::adresse).extracting(
            StrukturertAdresseformatDto::husnummerEtasjeLeilighet).containsExactlyInAnyOrder("42 C", null);
        assertThat(personopplysninger.bostedsadresser()).extracting(BostedsadresseDto::master)
            .containsExactlyInAnyOrder("NAV (PDL)", "");
        assertThat(personopplysninger.folkeregisteridentifikator()).isEqualTo("identNr");
        assertThat(personopplysninger.folkeregisterpersonstatuser()).containsExactly(
            new FolkeregisterpersonstatusDto(Personstatuser.UDEFINERT.getKode(), "ny status fra PDL", LocalDate.parse("2019-11-18"), LocalDate.parse("2029-11-18"), "NAV (PDL)", Master.PDL.name(), false));
        assertThat(personopplysninger.kjoenn()).isEqualTo(KjoennType.UKJENT);
        assertThat(personopplysninger.kontaktadresser()).hasSize(2);
        assertThat(personopplysninger.kontaktadresser()).extracting(KontaktadresseDto::master)
            .containsExactlyInAnyOrder("NAV (PDL)", "");
        assertThat(personopplysninger.navn()).isEqualTo(new NavnDto("Ola", "Oops", "King"));
        assertThat(personopplysninger.oppholdsadresser()).extracting(OppholdsadresseDto::adresse)
            .extracting(StrukturertAdresseformatDto::gatenavn).containsExactlyInAnyOrder("opphold 1", "opphold 2");
        assertThat(personopplysninger.oppholdsadresser()).extracting(OppholdsadresseDto::master)
            .containsExactlyInAnyOrder("NAV (PDL)", "");
        assertThat(personopplysninger.sivilstand()).flatExtracting(SivilstandDto::type,
            SivilstandDto::relatertVedSivilstand, SivilstandDto::gyldigFraOgMed, SivilstandDto::bekreftelsesdato,
            SivilstandDto::master, SivilstandDto::kilde, SivilstandDto::erHistorisk)
            .containsExactlyInAnyOrder("Registrert partner", "relatertVedSivilstandID", LocalDate.MIN, LocalDate.EPOCH, "NAV (PDL)",
                "kilde", false, "Udefinert type", "relatertVedSivilstandID", LocalDate.MIN, LocalDate.EPOCH, "NAV (PDL)", "kilde", false);

        Consumer<PersonopplysningerDto> statsborgerskapErSortert = personopplysningerDto -> {
            assertThat(personopplysningerDto.statsborgerskap().get(0).land()).isEqualTo("Testland C");
            assertThat(personopplysningerDto.statsborgerskap().get(0).master()).isEqualTo("NAV (PDL)");
            assertThat(personopplysningerDto.statsborgerskap().get(1).land()).isEqualTo("Testland A");
            assertThat(personopplysningerDto.statsborgerskap().get(2).land()).isEqualTo("Testland B");
        };
        assertThat(personopplysninger).isInstanceOfSatisfying(PersonopplysningerDto.class, statsborgerskapErSortert);
    }

    private static PersonMedHistorikk lagPersonMedHistorikk() {
        final var bostedsadresse_1 = new Bostedsadresse(
            new StrukturertAdresse("gate1", "42 C", null, null, null, null),
            null, null, null, "PDL", null, false);
        final var bostedsadresse_2 = new Bostedsadresse(
            new StrukturertAdresse("gate2", null, null, null, null, null),
            null, null, null, null, null, true);

        final var kontaktadresse_1 = new Kontaktadresse(
            new StrukturertAdresse("kontakt 1", null, null, null, null, null), null, null, null, null, "PDL", null, null,
            false);
        final var kontaktadresse_2 = new Kontaktadresse(null,
            new SemistrukturertAdresse("kontakt 2", "linje 2", null, null, "1234", "By", "IT"), null, null, null, null,
            null, null, false);

        final var oppholdsadresse_1 = new Oppholdsadresse(
            new StrukturertAdresse("opphold 1", null, null, null, null, null), null, null, null, "PDL", null, null,
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

        return new PersonMedHistorikk(Set.of(bostedsadresse_1, bostedsadresse_2),
            null, null, new Folkeregisteridentifikator("identNr"),
            Set.of(new Folkeregisterpersonstatus(Personstatuser.UDEFINERT, "ny status fra PDL", LocalDate.parse("2019-11-18"), LocalDate.parse("2029-11-18"), Master.PDL.name(), Master.PDL.name(), false)),
            KjoennType.UKJENT,
            Set.of(kontaktadresse_1, kontaktadresse_2), new Navn("Ola", "Oops", "King"),
            Set.of(oppholdsadresse_1, oppholdsadresse_2), Set.of(
            new Sivilstand(Sivilstandstype.REGISTRERT_PARTNER, null, "relatertVedSivilstandID", LocalDate.MIN,
                LocalDate.EPOCH, "PDL", "kilde", false),
            new Sivilstand(Sivilstandstype.UDEFINERT, "Udefinert type", "relatertVedSivilstandID", LocalDate.MIN,
                LocalDate.EPOCH, "PDL", "kilde", false)),
            Set.of(statsborgerskap_1, statsborgerskap_2, statsborgerskap_3));
    }
}
