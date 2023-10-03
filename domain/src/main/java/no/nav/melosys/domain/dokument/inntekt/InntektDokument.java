package no.nav.melosys.domain.dokument.inntekt;

import java.util.*;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import org.apache.commons.lang3.StringUtils;

public class InntektDokument implements SaksopplysningDokument {

    @NotNull
    public List<ArbeidsInntektMaaned> arbeidsInntektMaanedListe = new ArrayList<>();

    public List<ArbeidsInntektMaaned> getArbeidsInntektMaanedListe() {
        return arbeidsInntektMaanedListe;
    }

    public Set<String> hentOrgnumre() {
        return getArbeidsInntektMaanedListe().stream()
            .map(ArbeidsInntektMaaned::getArbeidsInntektInformasjon)
            .filter(Objects::nonNull)
            .map(ArbeidsInntektInformasjon::getInntektListe)
            .flatMap(Collection::stream)
            .map(Inntekt::getVirksomhetID)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toSet());
    }
}
