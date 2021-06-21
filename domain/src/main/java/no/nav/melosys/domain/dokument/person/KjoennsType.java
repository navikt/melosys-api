package no.nav.melosys.domain.dokument.person;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper;

public class KjoennsType extends AbstraktKodeverkHjelper {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public KjoennsType(@JsonProperty("kode") String kode) {
        super();
        this.kode = kode;
    }

    @Override
    public FellesKodeverk hentKodeverkNavn() {
        return FellesKodeverk.KJØNNSTYPER;
    }
}
