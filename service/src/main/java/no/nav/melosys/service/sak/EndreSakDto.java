package no.nav.melosys.service.sak;

import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;

public record EndreSakDto(
    Sakstyper sakstype,
    Sakstemaer sakstema
) {
}
