package no.nav.melosys.tjenester.gui.graphql;

import java.util.Comparator;
import java.util.stream.Collectors;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.graphql.dto.PersonopplysningerDto;
import no.nav.melosys.tjenester.gui.graphql.dto.SaksopplysningerDto;
import no.nav.melosys.tjenester.gui.graphql.dto.StatsborgerskapDto;
import no.nav.melosys.tjenester.gui.graphql.mapping.StatsborgerskapTilDtoConverter;
import org.springframework.stereotype.Component;

@Component
public class SaksopplysningerDataFetcher implements DataFetcher<Object> {
    private final BehandlingService behandlingService;
    private final KodeverkService kodeverkService;
    private final PersondataFasade persondataFasade;

    public SaksopplysningerDataFetcher(BehandlingService behandlingService,
                                       KodeverkService kodeverkService,
                                       PersondataFasade persondataFasade) {
        this.behandlingService = behandlingService;
        this.kodeverkService = kodeverkService;
        this.persondataFasade = persondataFasade;
    }

    @Override
    public DataFetcherResult<Object> get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        final Long behandlingID = dataFetchingEnvironment.getArgument("behandlingID");
        final String ident = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)
            .getFagsak().hentBruker().getAktørId();

        // TODO erstattes med hentPerson som tar behandlingID for å sjekke om data skal returneres fra gammel
        //  TPS-data eller fra PDL
        final var statsborgerskapDto = persondataFasade.hentStatsborgerskap(ident).stream()
            .map(s -> StatsborgerskapTilDtoConverter.tilDto(s, kodeverkService))
            .sorted(Comparator.comparing(StatsborgerskapDto::gyldigFraOgMed,
                Comparator.nullsFirst(Comparator.reverseOrder())))
            .collect(Collectors.toUnmodifiableList());
        return DataFetcherResult.newResult().data(new SaksopplysningerDto(behandlingID,
            new PersonopplysningerDto(statsborgerskapDto))).build();
    }
}
