package no.nav.melosys.tjenester.gui.graphql;

import java.util.List;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.graphql.dto.FamiliemedlemDto;
import no.nav.melosys.tjenester.gui.graphql.mapping.FamilemedlemTilDtoKonverter;
import org.springframework.stereotype.Component;

@Component
public class FamiliemedlemmerDataFetcher implements DataFetcher<List<FamiliemedlemDto>> {
    private final PersondataFasade persondataFasade;

    public FamiliemedlemmerDataFetcher(PersondataFasade persondataFasade) {
        this.persondataFasade = persondataFasade;
    }

    @Override
    public List<FamiliemedlemDto> get(DataFetchingEnvironment fetchingEnvironment) throws Exception {
        final Long behandlingID = fetchingEnvironment.getExecutionStepInfo().getParent().getParent().getArgument(
            "behandlingID");
        Objects.requireNonNull(behandlingID);
        return persondataFasade.hentFamiliemedlemmerMedHistorikk(behandlingID).stream().map(
            FamilemedlemTilDtoKonverter::tilDto).toList();
    }
}
