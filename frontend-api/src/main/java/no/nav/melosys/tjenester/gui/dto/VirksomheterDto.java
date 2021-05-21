package no.nav.melosys.tjenester.gui.dto;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VIRKSOMHET;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

public class VirksomheterDto {

    private List<String> virksomhetIDer;

    public List<String> getVirksomhetIDer() {
        return virksomhetIDer;
    }

    public void setVirksomhetIDer(List<String> virksomhetIDer) {
        this.virksomhetIDer = virksomhetIDer;
    }

    public static VirksomheterDto av(Set<AvklartefaktaDto> avklartefaktas) {
        List<String> virksomheter = new ArrayList<>();
        avklartefaktas.stream()
            .filter(avklartefakta -> VIRKSOMHET.getKode().equals(avklartefakta.getReferanse()) && VIRKSOMHET.equals(avklartefakta.getAvklartefaktaType()))
            .forEach(avklartefakta -> virksomheter.add(avklartefakta.getSubjektID()));

        VirksomheterDto virksomheterDto = new VirksomheterDto();
        virksomheterDto.setVirksomhetIDer(virksomheter);
        return virksomheterDto;
    }
}
