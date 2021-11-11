package no.nav.melosys.tjenester.gui.graphql;

import java.util.List;
import java.util.Set;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.graphql.dto.FamiliemedlemDto;
import no.nav.melosys.tjenester.gui.graphql.mapping.FamilemedlemTilDtoKonverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class FamiliemedlemmerDataFetcher implements DataFetcher<List<FamiliemedlemDto>> {
    private final PersondataFasade persondataFasade;

    public FamiliemedlemmerDataFetcher(PersondataFasade persondataFasade) {
        this.persondataFasade = persondataFasade;
    }

    @Override
    public List<FamiliemedlemDto> get(DataFetchingEnvironment fetchingEnvironment) throws Exception {
        Set<Familiemedlem> familiemedlemmer;
        final String ident = fetchingEnvironment.getExecutionStepInfo().getParent().getArgument("ident");
        if (!StringUtils.isEmpty(ident)) {
            familiemedlemmer = persondataFasade.hentFamiliemedlemmerMedHistorikk(ident);
        } else {
            final Long behandlingID = fetchingEnvironment.getExecutionStepInfo().getParent().getParent().getArgument(
                "behandlingID");
            familiemedlemmer = persondataFasade.hentFamiliemedlemmerMedHistorikk(behandlingID);
        }
        return familiemedlemmer.stream().map(
            FamilemedlemTilDtoKonverter::tilDto).toList();
    }
}
