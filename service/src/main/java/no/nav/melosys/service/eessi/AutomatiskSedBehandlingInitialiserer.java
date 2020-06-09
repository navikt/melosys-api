package no.nav.melosys.service.eessi;


import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.kontroll.PeriodeKontroller;

public interface AutomatiskSedBehandlingInitialiserer {

    RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) throws MelosysException;

    boolean gjelderSedType(SedType sedType);

    //Henter behandlingstype for spesifikk behandling av SED.
    //Kalles kun om det skal opprettes ny behandling for SED'en. Derfor greit at den kaster exception når det ikke støttes
    default Behandlingstema hentBehandlingstema(MelosysEessiMelding melosysEessiMelding) {
        throw new UnsupportedOperationException(
            "Nytt behandlingstema for initialiserer " + this.getClass().getSimpleName() + " støttes ikke"
        );
    }

    ProsessType hentAktuellProsessType();

    default boolean periodeErEndret(MelosysEessiMelding melosysEessiMelding, Behandlingsresultat behandlingsresultat) {
        Periode periode = tilPeriode(melosysEessiMelding.getPeriode());
        String lovvalgsLand = melosysEessiMelding.getLovvalgsland();

        return behandlingsresultat.finnValidertLovvalgsperiode().map(lovvalgsperiode ->
            !PeriodeKontroller.periodeErLik(lovvalgsperiode.getFom(), lovvalgsperiode.getTom(), periode.getFom(), periode.getTom())
                || !lovvalgsLand.equalsIgnoreCase(lovvalgsperiode.getLovvalgsland().getKode()))
            .orElse(true);
    }

    default Periode tilPeriode(no.nav.melosys.domain.eessi.Periode periode) {
        return new Periode(
            periode.getFom(),
            periode.getTom()
        );
    }
}
