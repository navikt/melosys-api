package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.List;

/**
 * FeltValgDto inneholder en liste over {@param valgAlternativer} brukeren må foreta. Feltet som bruker disse valgene
 * vil være usynlig med mindre brukeren velger et alternativ som har {@link FeltvalgAlternativDto#isVisFelt()} = true.
 */
public class FeltValgDto {

    private List<FeltvalgAlternativDto> valgAlternativer;
    private FeltValgType valgType;

    public FeltValgDto(List<FeltvalgAlternativDto> valgAlternativer, FeltValgType valgType) {
        this.valgAlternativer = valgAlternativer;
        this.valgType = valgType;
    }

    public List<FeltvalgAlternativDto> getValgAlternativer() {
        return valgAlternativer;
    }

    public void setValgAlternativer(List<FeltvalgAlternativDto> valgAlternativer) {
        this.valgAlternativer = valgAlternativer;
    }

    public FeltValgType getValgType() {
        return valgType;
    }

    public void setValgType(FeltValgType valgType) {
        this.valgType = valgType;
    }

}
