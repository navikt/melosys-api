package no.nav.melosys.regler.lovvalg.verifiser_inndata;


import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.VALIDERINGSFEIL;
import static no.nav.melosys.regler.lovvalg.LovvalgImparater.leggTilMelding;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.personopplysningDokumentet;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.søknadDokumentet;
import static no.nav.melosys.regler.motor.dekl.Deklarasjon.hvis;

import no.nav.melosys.regler.motor.Regel;
import no.nav.melosys.regler.motor.Regelpakke;

public class VerifiserPaakrevdeElementer extends Regelpakke {

    @Regel
    public static void skalAlltidHaSøknad() {
        utfør(
            hvis(søknadDokumentet()).mangler().så(leggTilMelding(VALIDERINGSFEIL, "Forespørselen mangler søknad"))
        );
    }
    
    @Regel
    public static void skalAlltidHaPersonopplysninger() {
        utfør(
            hvis(personopplysningDokumentet()).mangler().så(leggTilMelding(VALIDERINGSFEIL, "Forespørselen mangler personopplysninger"))
        );
    }
    
    // FIXME: Mer som skal inn her?
    
}
