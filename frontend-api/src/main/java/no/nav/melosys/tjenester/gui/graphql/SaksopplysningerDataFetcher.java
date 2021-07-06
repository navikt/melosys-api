package no.nav.melosys.tjenester.gui.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import no.nav.melosys.tjenester.gui.graphql.dto.SaksopplysningerDto;
import org.springframework.stereotype.Component;

@Component
public class SaksopplysningerDataFetcher implements DataFetcher<SaksopplysningerDto> {
    @Override
    public SaksopplysningerDto get(DataFetchingEnvironment fetchingEnvironment) throws Exception {
        final Long behandlingID = fetchingEnvironment.getArgument("behandlingID");
        return new SaksopplysningerDto(behandlingID);
    }
}
