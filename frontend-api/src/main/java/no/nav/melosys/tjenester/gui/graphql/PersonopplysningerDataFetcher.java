package no.nav.melosys.tjenester.gui.graphql;

import java.util.Comparator;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.graphql.dto.PersonopplysningerDto;
import no.nav.melosys.tjenester.gui.graphql.dto.SivilstandDto;
import no.nav.melosys.tjenester.gui.graphql.dto.StatsborgerskapDto;
import no.nav.melosys.tjenester.gui.graphql.mapping.*;
import org.apache.commons.lang3.StringUtils;
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
        PersonMedHistorikk personMedHistorikk;
        final String ident = fetchingEnvironment.getArgument("ident");
        if (!StringUtils.isEmpty(ident)) {
            personMedHistorikk = persondataFasade.hentPersonMedHistorikk(ident);
        } else {
            final Long behandlingID = fetchingEnvironment.getExecutionStepInfo().getParent().getArgument("behandlingID");
            personMedHistorikk = persondataFasade.hentPersonMedHistorikk(behandlingID);
        }
        Objects.requireNonNull(personMedHistorikk);

        final var bostedsadresseDtoList = personMedHistorikk.bostedsadresser().stream()
            .map(bostedsadresse -> BostedsadresseTilDtoKonverter.tilDto(bostedsadresse, kodeverkService)).toList();
        final var folkeregisterpersonstatusDtoList = personMedHistorikk.folkeregisterpersonstatuser().stream()
            .map(folkeregisterpersonstatus -> FolkeregisterpersonstatusTilDtoKonverter.tilDto(folkeregisterpersonstatus))
            .filter(status -> status != null).toList();
        final var kontaktadresseDtoList = personMedHistorikk.kontaktadresser().stream()
            .map(kontaktadresse -> KontaktadresseTilDtoKonverter.tilDto(kontaktadresse, kodeverkService)).toList();
        final var oppholdsadresseDtoList = personMedHistorikk.oppholdsadresser().stream()
            .map(oppholdsadresse -> OppholdsadresseTilDtoKonverter.tilDto(oppholdsadresse, kodeverkService)).toList();
        final var sivilstandDtoList = personMedHistorikk.sivilstand().stream()
            .map(SivilstandTilDtoKonverter::tilDto)
            .sorted(Comparator.comparing(SivilstandDto::gyldigFraOgMed,
                Comparator.nullsFirst(Comparator.reverseOrder())))
            .toList();
        final var statsborgerskapDtoList = personMedHistorikk.statsborgerskap().stream()
            .map(s -> StatsborgerskapTilDtoKonverter.tilDto(s, kodeverkService))
            .sorted(Comparator.comparing(StatsborgerskapDto::gyldigFraOgMed,
                Comparator.nullsFirst(Comparator.reverseOrder())))
            .toList();
        return new PersonopplysningerDto(bostedsadresseDtoList,
            FolkeregisteridentifikatorTilDtoKonverter.tilDto(personMedHistorikk.folkeregisteridentifikator()),
            folkeregisterpersonstatusDtoList,
            personMedHistorikk.kjønn(),
            kontaktadresseDtoList,
            NavnTilDtoKonverter.tilDto(personMedHistorikk.navn()),
            oppholdsadresseDtoList,
            sivilstandDtoList,
            statsborgerskapDtoList);
    }
}
