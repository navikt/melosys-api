package no.nav.melosys.tjenester.gui.graphql;

import java.util.Comparator;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.graphql.dto.PersonopplysningerDto;
import no.nav.melosys.tjenester.gui.graphql.dto.StatsborgerskapDto;
import no.nav.melosys.tjenester.gui.graphql.mapping.*;
import org.springframework.stereotype.Component;

@Component
public class PersonopplysningerDataFetcher implements DataFetcher<PersonopplysningerDto> {
    private final KodeverkService kodeverkService;
    private final PersondataFasade persondataFasade;

    public PersonopplysningerDataFetcher(KodeverkService kodeverkService, PersondataFasade persondataFasade) {
        this.kodeverkService = kodeverkService;
        this.persondataFasade = persondataFasade;
    }

    @Override
    public PersonopplysningerDto get(DataFetchingEnvironment fetchingEnvironment) throws Exception {
        final Long behandlingID = fetchingEnvironment.getExecutionStepInfo().getParent().getArgument("behandlingID");
        Objects.requireNonNull(behandlingID);

        final var personMedHistorikk = persondataFasade.hentPersonMedHistorikk(behandlingID);
        final var bostedsadresseDtoList = personMedHistorikk.bostedsadresser().stream()
            .map(bostedsadresse -> BostedsadresseTilDtoKonverter.tilDto(bostedsadresse, kodeverkService)).toList();
        final var kontaktadresseDtoList = personMedHistorikk.kontaktadresser().stream()
            .map(kontaktadresse -> KontaktadresseTilDtoKonverter.tilDto(kontaktadresse, kodeverkService)).toList();
        final var oppholdsadresseDtoList = personMedHistorikk.oppholdsadresser().stream()
            .map(oppholdsadresse -> OppholdsadresseTilDtoKonverter.tilDto(oppholdsadresse, kodeverkService)).toList();
        final var statsborgerskapDtoList = personMedHistorikk.statsborgerskap().stream()
            .map(s -> StatsborgerskapTilDtoKonverter.tilDto(s, kodeverkService))
            .sorted(Comparator.comparing(StatsborgerskapDto::gyldigFraOgMed,
                Comparator.nullsFirst(Comparator.reverseOrder())))
            .toList();
        return new PersonopplysningerDto(bostedsadresseDtoList,
            FolkeregisterpersonstatusTilDtoKonverter.tilDto(personMedHistorikk.folkeregisterpersonstatus()),
            personMedHistorikk.kjønn(),
            kontaktadresseDtoList,
            NavnTilDtoKonverter.tilDto(personMedHistorikk.navn()),
            oppholdsadresseDtoList,
            statsborgerskapDtoList);
    }
}
