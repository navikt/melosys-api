package no.nav.melosys.domain.dokument.medlemskap;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

import static java.util.function.Predicate.not;

public class MedlemskapDokument implements SaksopplysningDokument {

    public List<Medlemsperiode> medlemsperiode = new ArrayList<>();

    public List<Medlemsperiode> getMedlemsperiode() {
        return medlemsperiode;
    }

    public List<Medlemsperiode> hentMedlemsperioderHvorKildeIkkeLånekassen() {
        return medlemsperiode.stream()
            .filter(not(Medlemsperiode::erKildeLånekassen))
            .toList();
    }
}
