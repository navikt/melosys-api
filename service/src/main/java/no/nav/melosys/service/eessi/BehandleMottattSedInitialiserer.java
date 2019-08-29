package no.nav.melosys.service.eessi;


import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;

public interface BehandleMottattSedInitialiserer {

    RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) throws TekniskException, FunksjonellException;

    boolean gjelderSedType(SedType sedType);

    Behandlingstyper hentBehandlingstype(MelosysEessiMelding melosysEessiMelding);

    ProsessType hentAktuellProsessType();
}
