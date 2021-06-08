package no.nav.melosys.tjenester.gui.graphql;

import java.util.stream.Collectors;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.tjenester.gui.graphql.dto.PersonopplysningerDto;
import no.nav.melosys.tjenester.gui.graphql.dto.SaksopplysningerDto;
import no.nav.melosys.tjenester.gui.graphql.mapping.StatsborgerskapTilDtoConverter;
import org.springframework.stereotype.Component;

@Component
public class SaksopplysningerDataFetcher implements DataFetcher<Object> {
    private final FagsakService fagsakService;
    private final PersondataFasade persondataFasade;

    public SaksopplysningerDataFetcher(FagsakService fagsakService, PersondataFasade persondataFasade) {
        this.fagsakService = fagsakService;
        this.persondataFasade = persondataFasade;
    }

    @Override
    public DataFetcherResult<Object> get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        final String saksnummer = dataFetchingEnvironment.getArgument("saksnummer");
        final String ident = fagsakService.hentFagsak(saksnummer).hentBruker().getAktørId();

        final var statsborgerskapDto = persondataFasade.hentStatsborgerskap(ident).stream().map(
            StatsborgerskapTilDtoConverter::tilDto).collect(Collectors.toUnmodifiableList());
        return DataFetcherResult.newResult().data(new SaksopplysningerDto(saksnummer,
            new PersonopplysningerDto(statsborgerskapDto))).build();
    }
}
