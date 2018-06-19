package no.nav.melosys.saksflyt.agent.unntak;

import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.saksflyt.agent.UnntakBehandler;

import static no.nav.melosys.saksflyt.agent.unntak.KjedetUnntakBehandler.først;
import static no.nav.melosys.saksflyt.agent.unntak.OpprettHendelse.opprettHendelse;
import static no.nav.melosys.saksflyt.agent.unntak.SettTilFeilet.settTilFeilet;
import static no.nav.melosys.saksflyt.agent.unntak.StandardFeilStrategi.Feilkategori.*;

public class StandardFeilStrategi {
    
    public static enum Feilkategori {TEKNISK_FEIL, FUNKSJONELL_FEIL, REGELMODULEN_RETURNETE_FEIL, EXCEPTION;};
    
    private StandardFeilStrategi() {}
    
    /**
     * Returnerer standard strategi for feilhåndtering.
     */
    public static Map<Object, UnntakBehandler> standardFeilHåndtering() {
        Map<Object, UnntakBehandler> res = new HashMap<>();
        res.put(TEKNISK_FEIL, 
            først(opprettHendelse("Teknisk feil", "Teknisk feil")).
            så(settTilFeilet())
        );
        res.put(FUNKSJONELL_FEIL, 
            først(opprettHendelse("Funksjonell feil", "Funksjonell feil")).
            så(settTilFeilet())
        );
        res.put(REGELMODULEN_RETURNETE_FEIL, 
            først(opprettHendelse("Funksjonell feil", "Regelmodulen returnerte feilmelding")).
            så(settTilFeilet())
        );
        res.put(EXCEPTION, ExceptionBehandler.exceptionBehandler());
        return res;
    }

}
