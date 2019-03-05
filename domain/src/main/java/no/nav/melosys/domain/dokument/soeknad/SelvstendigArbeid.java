package no.nav.melosys.domain.dokument.soeknad;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SelvstendigArbeid {
    public Boolean erSelvstendig;
    public List<SelvstendigForetak> selvstendigForetak = new ArrayList<>();

    public Stream<String> hentAlleOrganisasjonsnumre() {
        return selvstendigForetak.stream().map(sf -> sf.orgnr);
    }
}
