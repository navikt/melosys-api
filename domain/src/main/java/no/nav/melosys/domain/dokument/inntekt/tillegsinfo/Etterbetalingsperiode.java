package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.inntekt.Periode;

public class Etterbetalingsperiode extends TilleggsinformasjonDetaljer implements HarPeriode {

    public Periode etterbetalingsperiode;

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return etterbetalingsperiode;
    }
}
