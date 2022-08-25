package no.nav.melosys.service.sak;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.FunksjonellException;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.*;

public class MuligeManuelleFagsakEndringer {
    public static Set<Sakstemaer> hentMuligeSakstema(Behandling behandling) {

        if (behandling.kanIkkeEndres()) {
            return Collections.emptySet();
        }

        return switch (behandling.getFagsak().getTema()) {
            case MEDLEMSKAP_LOVVALG -> Set.of(UNNTAK, TRYGDEAVGIFT);
            case UNNTAK -> Set.of(MEDLEMSKAP_LOVVALG, TRYGDEAVGIFT);
            case TRYGDEAVGIFT -> Set.of(UNNTAK, MEDLEMSKAP_LOVVALG);
            default -> Collections.emptySet();
        };
    }

    public static Set<Sakstyper> hentMuligeSakstype(Behandling behandling) {
        return Collections.emptySet();

        /*
        TODO: Endre sakstype fikses i MELOSYS-5285
        if (behandling.kanIkkeEndres()) {
            return Collections.emptySet();
        }

        return switch (behandling.getFagsak().getType()) {
            case EU_EOS -> Set.of(Sakstyper.TRYGDEAVTALE, Sakstyper.FTRL);
            case TRYGDEAVTALE -> Set.of(Sakstyper.EU_EOS, Sakstyper.FTRL);
            case FTRL -> Set.of(Sakstyper.EU_EOS, Sakstyper.TRYGDEAVTALE);
            default -> Collections.emptySet();
        };
        */
    }

    public static void validerNySakstemaMulig(Behandling behandling, Sakstemaer sakstemaer) {
        if (!hentMuligeSakstema(behandling).contains(sakstemaer)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til sakstema %s. Gyldige sakstema for behandling %s er %s",
                sakstemaer, behandling.getId(), hentMuligeSakstema(behandling)));
        }
    }

    public static void validerNySakstypeMulig(Behandling behandling, Sakstyper sakstype) {
        if (!hentMuligeSakstype(behandling).contains(sakstype)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til sakstype %s. Gyldige sakstype for behandling %s er %s",
                sakstype, behandling.getId(), hentMuligeSakstype(behandling)));
        }
    }
}
