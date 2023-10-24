package no.nav.melosys.tjenester.gui.dto.oppsummertefakta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.ARBEIDSLAND;

public class ArbeidslandDto {

    private List<String> arbeidsland;

    public List<String> getArbeidsland() {
        return arbeidsland;
    }

    public void setArbeidsland(List<String> arbeidsland) {
        this.arbeidsland = arbeidsland;
    }

    public static ArbeidslandDto av(Set<AvklartefaktaDto> avklartefaktas) {
        List<String> land = new ArrayList<>();
        avklartefaktas.stream()
            .filter(avklartefakta -> ARBEIDSLAND.getKode().equals(avklartefakta.getReferanse())
                && ARBEIDSLAND.equals(avklartefakta.getAvklartefaktaType()))
            .forEach(avklartefakta -> land.add(avklartefakta.getSubjektID()));

        ArbeidslandDto arbeidslandDto = new ArbeidslandDto();
        arbeidslandDto.setArbeidsland(land);
        return arbeidslandDto;
    }
}
