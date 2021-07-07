package no.nav.melosys.tjenester.gui.graphql;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.graphql.dto.PersonopplysningerDto;
import no.nav.melosys.tjenester.gui.graphql.dto.StatsborgerskapDto;
import no.nav.melosys.tjenester.gui.graphql.mapping.BostedsadresseTilDtoConverter;
import no.nav.melosys.tjenester.gui.graphql.mapping.StatsborgerskapTilDtoConverter;
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
            .map(BostedsadresseTilDtoConverter::tilDto)
            .collect(Collectors.toUnmodifiableList());
        final var statsborgerskapDtoList = personMedHistorikk
            .statsborgerskap().stream()
            .map(s -> StatsborgerskapTilDtoConverter.tilDto(s, kodeverkService))
            .sorted(Comparator.comparing(StatsborgerskapDto::gyldigFraOgMed,
                Comparator.nullsFirst(Comparator.reverseOrder())))
            .collect(Collectors.toUnmodifiableList());
        return new PersonopplysningerDto(bostedsadresseDtoList, statsborgerskapDtoList);
    }
}
