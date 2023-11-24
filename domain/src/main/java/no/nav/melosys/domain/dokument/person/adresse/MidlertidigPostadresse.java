package no.nav.melosys.domain.dokument.person.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;




public class MidlertidigPostadresse {

    public LocalDateTime endringstidspunkt;

    public Land land;
    public Periode postleveringsPeriode;

}
