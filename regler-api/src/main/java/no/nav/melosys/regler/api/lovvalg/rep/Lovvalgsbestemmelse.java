package no.nav.melosys.regler.api.lovvalg.rep;

import java.util.List;

public class Lovvalgsbestemmelse {
    
    /** Artikkel/lovhjemmel */
    public Artikkel artikkel;
    
    /** Liste med betingelser som bestemmer im artikkelen skal invokeres */
    public List<Betingelse> betingelser;
    
}
