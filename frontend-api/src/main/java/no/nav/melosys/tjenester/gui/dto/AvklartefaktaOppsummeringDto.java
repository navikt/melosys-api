package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.ArbeidslandDto;

public class AvklartefaktaOppsummeringDto {

    private VirksomheterDto virksomheter;
    private ArbeidslandDto arbeidsland;

    public VirksomheterDto getVirksomheter() {
        return virksomheter;
    }

    public void setVirksomheter(VirksomheterDto virksomheter) {
        this.virksomheter = virksomheter;
    }

    public static AvklartefaktaOppsummeringDto av(Set<AvklartefaktaDto> avklartefakta) {
        AvklartefaktaOppsummeringDto avklartefaktaOppsummeringDto = new AvklartefaktaOppsummeringDto();
        avklartefaktaOppsummeringDto.setVirksomheter(VirksomheterDto.av(avklartefakta));
        avklartefaktaOppsummeringDto.setArbeidsland(ArbeidslandDto.av(avklartefakta));
        return avklartefaktaOppsummeringDto;
    }

    public ArbeidslandDto getArbeidsland() {
        return arbeidsland;
    }

    public void setArbeidsland(ArbeidslandDto arbeidsland) {
        this.arbeidsland = arbeidsland;
    }
}
