package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.List;

public class FeltValgDto {

    private List<FeltvalgAlternativDto> valgAltnerativer;
    private FeltValgType valgType;
    private FeltvalgAlternativDto valgAlternativTrigger;

    public FeltValgDto(List<FeltvalgAlternativDto> valgAltnerativer, FeltValgType valgType, FeltvalgAlternativDto valgAlternativTrigger){
        this.valgAltnerativer = valgAltnerativer;
        this.valgType = valgType;
        this.valgAlternativTrigger = valgAlternativTrigger;
    }

    public List<FeltvalgAlternativDto> getValgAltnerativer() {
        return valgAltnerativer;
    }

    public void setValgAltnerativer(List<FeltvalgAlternativDto> valgAltnerativer) {
        this.valgAltnerativer = valgAltnerativer;
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
