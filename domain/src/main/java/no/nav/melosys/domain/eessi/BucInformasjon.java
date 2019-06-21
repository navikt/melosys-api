package no.nav.melosys.domain.eessi;

import java.time.LocalDate;
import java.util.List;

public class BucInformasjon {

    private final String id;
    private final String bucType;
    private final LocalDate opprettetDato;
    private final List<SedInformasjon> seder;

    public BucInformasjon(String id, String bucType, LocalDate opprettetDato, List<SedInformasjon> seder) {
        this.id = id;
        this.bucType = bucType;
        this.opprettetDato = opprettetDato;
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

    public List<SedInformasjon> getSeder() {
        return seder;
    }
}
