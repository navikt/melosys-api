package no.nav.melosys.domain.dokument.person;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper;
import no.nav.melosys.domain.person.Sivilstandstype;
import org.apache.commons.lang3.StringUtils;

public class Sivilstand extends AbstraktKodeverkHjelper {
    @Override
    public FellesKodeverk hentKodeverkNavn() {
        return FellesKodeverk.SIVILSTANDER;
    }

    public Sivilstandstype tilSivilstandstypeFraDomene() {
        final String kode = getKode();
        if (StringUtils.isEmpty(kode)) {
            return Sivilstandstype.UDEFINERT;
        }
        return switch (kode) {
            case "ENKE" -> Sivilstandstype.ENKE_ELLER_ENKEMANN;
            case "GIFT" -> Sivilstandstype.GIFT;
            case "GJPA" -> Sivilstandstype.GJENLEVENDE_PARTNER;
            case "NULL" -> Sivilstandstype.UOPPGITT;
            case "REPA" -> Sivilstandstype.REGISTRERT_PARTNER;
            case "SEPA" -> Sivilstandstype.SEPARERT_PARTNER;
            case "SEPR" -> Sivilstandstype.SEPARERT;
            case "SKIL" -> Sivilstandstype.SKILT;
            case "SKPA" -> Sivilstandstype.SKILT_PARTNER;
            case "UGI" -> Sivilstandstype.UGIFT;
            default -> Sivilstandstype.UDEFINERT;
        };
    }
}
