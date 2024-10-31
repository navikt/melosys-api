package no.nav.melosys.service.eessi.ruting;

import java.util.Collection;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;

import static no.nav.melosys.service.kontroll.regler.PeriodeRegler.periodeLandErLik;

public interface SedRuterForSedTyper extends SedRuter {

    Collection<SedType> gjelderSedTyper();

    default boolean periodeErEndret(MelosysEessiMelding melosysEessiMelding, Behandlingsresultat behandlingsresultat) {
        Periode periode = tilPeriode(melosysEessiMelding.getPeriode());
        String lovvalgsLand = melosysEessiMelding.getLovvalgsland();

        return behandlingsresultat.finnLovvalgsperiode().map(lovvalgsperiode ->
                !PeriodeRegler.periodeErLik(lovvalgsperiode.getFom(), lovvalgsperiode.getTom(), periode.getFom(), periode.getTom())
                    || !periodeLandErLik(lovvalgsperiode.getLovvalgsland(), lovvalgsLand))
            .orElse(true);
    }

    default Periode tilPeriode(no.nav.melosys.domain.eessi.Periode periode) {
        return new Periode(
            periode.getFom(),
            periode.getTom()
        );
    }
}
