package no.nav.melosys.tjenester.gui.graphql;

import java.util.List;
import java.util.Set;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.tjenester.gui.graphql.dto.FamiliemedlemDto;
import no.nav.melosys.tjenester.gui.graphql.mapping.FamiliemedlemTilDtoKonverter;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
public class FamiliemedlemmerDataFetcher implements DataFetcher<List<FamiliemedlemDto>> {
    private final PersondataFasade persondataFasade;

    public FamiliemedlemmerDataFetcher(PersondataFasade persondataFasade) {
        this.persondataFasade = persondataFasade;
    }

    @Override
    public List<FamiliemedlemDto> get(DataFetchingEnvironment fetchingEnvironment) throws Exception {
        return hentFamiliemedlemmer(fetchingEnvironment).stream()
            .map(FamiliemedlemTilDtoKonverter::tilDto)
            .toList();
    }

    private Set<Familiemedlem> hentFamiliemedlemmer(DataFetchingEnvironment fetchingEnvironment) {
        final String ident = hentIdent(fetchingEnvironment);
        if (isNotEmpty(ident)) {
            return persondataFasade.hentFamiliemedlemmerFraIdent(ident);
        }

        final Long behandlingID = hentBehandlingID(fetchingEnvironment);
        return persondataFasade.hentFamiliemedlemmerFraBehandlingID(behandlingID);
    }

    private String hentIdent(DataFetchingEnvironment fetchingEnvironment) {
        return fetchingEnvironment.getExecutionStepInfo().getParent().getArgument("ident");
    }

    private Long hentBehandlingID(DataFetchingEnvironment fetchingEnvironment) {
        return fetchingEnvironment.getExecutionStepInfo().getParent().getParent().getArgument("behandlingID");
    }
}
