package no.nav.melosys.tjenester.gui.dto;

import jakarta.annotation.Nullable;

// Alle felt er nullable: skjemaer som ikke har et fritekstfelt (f.eks. opphørsvedtak uten innledning)
// autolagrer uten det, og autolagring skal aldri feile (MELOSYS-8141).
public record LagreFritekstDto(@Nullable String innledningFritekst,
                               @Nullable String begrunnelseFritekst,
                               @Nullable String trygdeavgiftFritekst) {
}
