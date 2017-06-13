package no.nav.melosys.regler.api.lovvalg;

<<<<<<< HEAD
import java.time.LocalDate;
import java.util.List;

import com.google.gson.Gson;

=======
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb
/**
 * DTO for forespørsler til lovvalgtjenesten
 */
public class FastsettLovvalgRequest {
<<<<<<< HEAD
    
    // Navn, adresse etc. har ingen effekt på utfallet
    
    // FIXME (farjam 2017-06-12): Javadoc
    // FIXME (farjam 2017-06-06): Legg til kodeverk for land etc.
    // FIXME (farjam 2017-06-06): Flat... legg til struktur
    
    public String statsborgerskap;
    
    public boolean arbeidstakerEllerSelvstendigNaeringsdrivende;
    public boolean arbeidFlereLand;
    public boolean arbeidstakerOgSelvstendigNaeringsdrivende;
    public boolean arbeidSkip;
    public String skipFlaggland;
    public boolean arbeidInternasjonalTransport;
    public boolean arbeidUdForsvaret;
    public boolean arbeidSokkel;
    public String sokkelLand;
    public String annet;
    
    public LocalDate periodeFom;
    public LocalDate periodeTom;
    
    
    
    public List<String> land;
    
    public boolean utsendtForAaErstatteEnAnnenPerson;
    
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
=======
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb

}
