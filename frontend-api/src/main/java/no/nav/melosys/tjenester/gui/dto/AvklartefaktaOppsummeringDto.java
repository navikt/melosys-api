package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

public class AvklartefaktaOppsummeringDto {

    private VirksomheterDto virksomheter;
    private MedfolgendeFamilieDto medfolgendeFamilie;

    public MedfolgendeFamilieDto getMedfolgendeFamilie() {
        return medfolgendeFamilie;
    }

    public VirksomheterDto getVirksomheter() {
        return virksomheter;
    }

    public void setMedfolgendeFamilie(MedfolgendeFamilieDto medfolgendeFamilie) {
        this.medfolgendeFamilie = medfolgendeFamilie;
    }

    public void setVirksomheter(VirksomheterDto virksomheter) {
        this.virksomheter = virksomheter;
    }

    public static AvklartefaktaOppsummeringDto av(Set<AvklartefaktaDto> avklartefakta) {
        AvklartefaktaOppsummeringDto avklartefaktaOppsummeringDto = new AvklartefaktaOppsummeringDto();
        avklartefaktaOppsummeringDto.setVirksomheter(VirksomheterDto.tilVirksomheterDto(avklartefakta));
        avklartefaktaOppsummeringDto.setMedfolgendeFamilie(MedfolgendeFamilieDto.tilMedfolgendeFamilieDto(avklartefakta));
        return avklartefaktaOppsummeringDto;
    }
}
