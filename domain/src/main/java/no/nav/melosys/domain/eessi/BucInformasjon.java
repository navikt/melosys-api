package no.nav.melosys.domain.eessi;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class BucInformasjon {

    private final String id;
    private final String bucType;
    private final LocalDate opprettetDato;
    private final Set<String> mottakerinstitusjoner;
    private final List<SedInformasjon> seder;

    public BucInformasjon(String id, String bucType, LocalDate opprettetDato, Set<String> mottakerinstitusjoner, List<SedInformasjon> seder) {
        this.id = id;
        this.bucType = bucType;
        this.opprettetDato = opprettetDato;
        this.mottakerinstitusjoner = mottakerinstitusjoner;
        this.seder = seder;
    }

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
