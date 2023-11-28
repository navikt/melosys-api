package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.time.YearMonth;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.felles.Periode;

public class Utenlandsopphold {

    private Periode periode;

    private String land;

    @JsonProperty("rapporteringsAarMaaned")
    private YearMonth rapporteringsperiode;

    /** Obs. Ikke kodeverk! */
    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public YearMonth getRapporteringsperiode() {
        return rapporteringsperiode;
    }

    public void setRapporteringsperiode(YearMonth rapporteringsperiode) {
        this.rapporteringsperiode = rapporteringsperiode;
    }
}
