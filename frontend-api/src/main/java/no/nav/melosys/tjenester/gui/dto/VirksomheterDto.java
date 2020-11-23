package no.nav.melosys.tjenester.gui.dto;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VIRKSOMHET;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

public class VirksomheterDto {

    private List<String> orgnummer;

    public List<String> getOrgnummer() {
        return orgnummer;
    }

    public void setOrgnummer(List<String> orgnummer) {
        this.orgnummer = orgnummer;
    }

    public static VirksomheterDto tilVirksomheterDto(Set<AvklartefaktaDto> avklartefaktas) {
        List<String> virksomheter = new ArrayList<>();
        avklartefaktas.stream()
            .filter(avklartefakta -> VIRKSOMHET.getKode().equals(avklartefakta.getReferanse()) && VIRKSOMHET.equals(avklartefakta.getAvklartefaktaType()))
            .forEach(avklartefakta -> virksomheter.add(avklartefakta.getSubjektID()));

        VirksomheterDto virksomheterDto = new VirksomheterDto();
        virksomheterDto.setOrgnummer(virksomheter);
        return virksomheterDto;
    }
}
