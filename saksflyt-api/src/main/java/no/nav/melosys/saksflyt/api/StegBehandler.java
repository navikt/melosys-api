package no.nav.melosys.saksflyt.api;

import java.util.function.Predicate;

import no.nav.melosys.domain.saksflyt.Prosessinstans;

/**
 * Interface for alle agenter som utfører maskinelle steg.
 *
 * Alle implementasjoner må være trådsikre.
 */
public interface StegBehandler {

    /**
     * Predikat som kvalifiserer Prosessinstanser for denne Stegbehandleren
     */
    Predicate<Prosessinstans> inngangsvilkår();

    /**
     * Kalles av arbeidertråder for å gi StegBehandleren et stykke arbeid. Metoden skal kunne kalles parallelt.
     * Implementasjonen må sørge for at et kall ikke tar for lang tid (i alle fall ikke mer enn ett minutt).
     * Se MELOSYS-1326: (Få agenter til å kjøre i egen tråd).
     */
    void utførSteg(Prosessinstans prosessinstans);

}
