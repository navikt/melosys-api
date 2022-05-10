package no.nav.melosys.integrasjon.pdl.dto.person;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Sivilstand(Sivilstandstype type,
                         String relatertVedSivilstand,
                         LocalDate gyldigFraOgMed,
                         LocalDate bekreftelsesdato,
                         Metadata metadata) implements HarMetadata {

    public boolean erGyldigSomEktefelleEllerPartner() {
        return erIkkeHistorisk() && Objects.nonNull(relatertVedSivilstand) &&
            (Sivilstandstype.GIFT.equals(type) || Sivilstandstype.REGISTRERT_PARTNER.equals(type));
    }
}
