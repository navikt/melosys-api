package no.nav.melosys.integrasjon.pdl.dto.person;

import java.time.LocalDate;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

import static java.util.Objects.nonNull;

public record Sivilstand(Sivilstandstype type,
                         String relatertVedSivilstand,
                         LocalDate gyldigFraOgMed,
                         LocalDate bekreftelsesdato,
                         Metadata metadata) implements HarMetadata {

    public boolean erGyldigForEktefelleEllerPartner() {
        return erIkkeHistorisk() && nonNull(relatertVedSivilstand) && harGyldigSivilstandstype();
    }

    private boolean harGyldigSivilstandstype() {
        return Sivilstandstype.GIFT.equals(type)
            || Sivilstandstype.SEPARERT.equals(type)
            || Sivilstandstype.SEPARERT_PARTNER.equals(type)
            || Sivilstandstype.REGISTRERT_PARTNER.equals(type);
    }
}
