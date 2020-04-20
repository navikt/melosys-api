package no.nav.melosys.saksflyt.steg.unntak;

import java.util.EnumMap;
import java.util.Map;

import no.nav.melosys.saksflyt.feil.Feilkategori;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;

import static no.nav.melosys.saksflyt.feil.Feilkategori.*;
import static no.nav.melosys.saksflyt.steg.unntak.KjedetUnntakBehandler.først;
import static no.nav.melosys.saksflyt.steg.unntak.OpprettHendelse.opprettHendelse;
import static no.nav.melosys.saksflyt.steg.unntak.SettTilFeilet.settTilFeilet;

public class FeilStrategi {

    private FeilStrategi() {
    }

    /**
     * Returnerer standard strategi for feilhåndtering.
     */
    public static Map<Feilkategori, UnntakBehandler> standardFeilHåndtering() {
        Map<Feilkategori, UnntakBehandler> res = new EnumMap<>(Feilkategori.class);
        res.put(TEKNISK_FEIL,
            først(opprettHendelse("Teknisk feil")).
                så(settTilFeilet())
        );
        res.put(INTEGRASJON_FEIL,
            først(opprettHendelse("Integrasjonsfeil")).
                så(settTilFeilet())
        );
        res.put(UVENTET_EXCEPTION, ExceptionBehandler.exceptionBehandler());
        res.put(FUNKSJONELL_FEIL,
            først(opprettHendelse("Funksjonell feil")).
                så(settTilFeilet())
        );
        res.put(INGEN_TILGANG,
            først(opprettHendelse("Ingen tilgang")).
                så(settTilFeilet())
        );
        res.put(IKKE_FUNNET,
            først(opprettHendelse("Ikke funnet")).
                så(settTilFeilet())
        );
        return res;
    }

}
