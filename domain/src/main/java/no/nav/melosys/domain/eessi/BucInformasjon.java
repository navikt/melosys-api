package no.nav.melosys.domain.eessi;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record BucInformasjon(String id, boolean erÅpen, String bucType,
                             LocalDate opprettetDato, Set<String> mottakerinstitusjoner,
                             List<SedInformasjon> seder) {

    public String getId() {
        return id;
    }

    public String getBucType() {
        return bucType;
    }

    public LocalDate getOpprettetDato() {
        return opprettetDato;
    }

    public Set<String> getMottakerinstitusjoner() {
        return mottakerinstitusjoner;
    }

    public List<SedInformasjon> getSeder() {
        return seder;
    }
}
