package no.nav.melosys.regler.api.lovvalg.old;

import java.util.List;

import com.google.gson.Gson;

public class Lovvalgsbestemmelse {
    
    /** Artikkel/lovhjemmel */
    public Artikkel artikkel;
    
    /** Liste med betingelser for artikkelen */
    public List<Betingelse> betingelser;
    
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
