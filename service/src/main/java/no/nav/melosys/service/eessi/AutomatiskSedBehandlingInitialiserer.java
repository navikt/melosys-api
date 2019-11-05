package no.nav.melosys.service.eessi;


import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;

public interface AutomatiskSedBehandlingInitialiserer {

    RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) throws TekniskException, FunksjonellException;

    boolean gjelderSedType(SedType sedType, Landkoder lovvalgsland);

    //Henter behandlingstype for spesifikk behandling av SED.
    //Kalles kun om det skal opprettes ny behandling for SED'en. Derfor greit at den kaster exception når det ikke støttes
    default Behandlingstyper hentBehandlingstype(MelosysEessiMelding melosysEessiMelding) {
        throw new UnsupportedOperationException(
            "Ny behandlingstype for initialiserer " + this.getClass().getSimpleName() + " støttes ikke"
        );
    }

    ProsessType hentAktuellProsessType();
}
