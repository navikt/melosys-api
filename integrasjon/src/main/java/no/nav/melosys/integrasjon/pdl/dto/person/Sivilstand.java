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

    public boolean erAktiv() {
        return erIkkeHistorisk() && Objects.nonNull(relatertVedSivilstand);
    }

    @Override
    public String toString() {
        return "Sivilstand{" +
            "type=" + type +
            ", relatertVedSivilstand='" + relatertVedSivilstand + '\'' +
            ", gyldigFraOgMed=" + gyldigFraOgMed +
            ", bekreftelsesdato=" + bekreftelsesdato +
            ", metadata=" + metadata +
            '}';
    }
}
