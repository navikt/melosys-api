package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.List;

public class FeltValgDto {

    private List<FeltvalgAlternativDto> valgAlternativer;
    private FeltValgType valgType;
    private FeltvalgAlternativDto valgAlternativTrigger;

    public FeltValgDto(List<FeltvalgAlternativDto> valgAlternativer, FeltValgType valgType, FeltvalgAlternativDto valgAlternativTrigger){
        this.valgAlternativer = valgAlternativer;
        this.valgType = valgType;
        this.valgAlternativTrigger = valgAlternativTrigger;
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

    public FeltvalgAlternativDto getValgAlternativTrigger() {
        return valgAlternativTrigger;
    }

    public void setValgAlternativTrigger(FeltvalgAlternativDto valgAlternativTrigger) {
        this.valgAlternativTrigger = valgAlternativTrigger;
    }
}
