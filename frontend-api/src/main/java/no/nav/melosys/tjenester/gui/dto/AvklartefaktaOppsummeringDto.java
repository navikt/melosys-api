package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

public class AvklartefaktaOppsummeringDto {

    private VirksomheterDto virksomheter;
    private Set<MedfolgendeFamilieDto> medfolgendeFamilie;

    public Set<MedfolgendeFamilieDto> getMedfolgendeFamilie() {
        return medfolgendeFamilie;
    }

    public VirksomheterDto getVirksomheter() {
        return virksomheter;
    }

    public void setMedfolgendeFamilie(Set<MedfolgendeFamilieDto> medfolgendeFamilie) {
        this.medfolgendeFamilie = medfolgendeFamilie;
    }

    public void setVirksomheter(VirksomheterDto virksomheter) {
        this.virksomheter = virksomheter;
    }

    public static AvklartefaktaOppsummeringDto av(Set<AvklartefaktaDto> avklartefakta) {
        AvklartefaktaOppsummeringDto avklartefaktaOppsummeringDto = new AvklartefaktaOppsummeringDto();
        avklartefaktaOppsummeringDto.setVirksomheter(VirksomheterDto.av(avklartefakta));
        avklartefaktaOppsummeringDto.setMedfolgendeFamilie(MedfolgendeFamilieDto.av(avklartefakta));
        return avklartefaktaOppsummeringDto;
    }
}
