package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

public class AvklartefaktaOppsummeringDto {

    private VirksomheterDto virksomheter;

    public VirksomheterDto getVirksomheter() {
        return virksomheter;
    }

    public void setVirksomheter(VirksomheterDto virksomheter) {
        this.virksomheter = virksomheter;
    }

    public static AvklartefaktaOppsummeringDto tilAvklartefaktaStrukturertDto(Set<AvklartefaktaDto> avklartefakta) {
        AvklartefaktaOppsummeringDto avklartefaktaOppsummeringDto = new AvklartefaktaOppsummeringDto();
        avklartefaktaOppsummeringDto.setVirksomheter(VirksomheterDto.tilVirksomheterDto(avklartefakta));
        return avklartefaktaOppsummeringDto;
    }
}
