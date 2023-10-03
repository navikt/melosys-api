package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.inntekt.Periode;

public class BarnepensjonOgUnderholdsbidrag extends TilleggsinformasjonDetaljer implements HarPeriode {

    public String forsørgersFødselnummer;

    public Periode tidsrom;

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return tidsrom;
    }
}
