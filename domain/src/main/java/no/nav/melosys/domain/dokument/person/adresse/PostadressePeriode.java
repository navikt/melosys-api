package no.nav.melosys.domain.dokument.person.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;

public class PostadressePeriode implements HarPeriode {

    public Periode periode;

    public LocalDateTime endringstidspunkt;

    public UstrukturertAdresse postadresse;

    @Override
    public ErPeriode getPeriode() {
        return periode;
    }
}
