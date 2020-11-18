package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

public class AvklartefaktaStrukturertDto {

    private VirksomheterDto virksomheter;

    public VirksomheterDto getVirksomheter() {
        return virksomheter;
    }

    public void setVirksomheter(VirksomheterDto virksomheter) {
        this.virksomheter = virksomheter;
    }

    public static AvklartefaktaStrukturertDto tilAvklartefaktaStrukturertDto(Set<AvklartefaktaDto> avklartefakta) {
        AvklartefaktaStrukturertDto avklartefaktaStrukturertDto = new AvklartefaktaStrukturertDto();
        avklartefaktaStrukturertDto.setVirksomheter(VirksomheterDto.tilVirksomheterDto(avklartefakta));
        return avklartefaktaStrukturertDto;
    }
}
