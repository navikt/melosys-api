package no.nav.melosys.tjenester.gui.dto;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;

public class BehandlingsresultatDto {

    private List<String> begrunnelser;
    private String begrunnelseFritekst;

    public List<String> getBegrunnelser() {
        return begrunnelser;
    }

    public void setBegrunnelser(List<String> begrunnelser) {
        this.begrunnelser = begrunnelser;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public static BehandlingsresultatDto av(Behandlingsresultat behandlingsresultat) {
        BehandlingsresultatDto dto = new BehandlingsresultatDto();
        dto.setBegrunnelseFritekst(behandlingsresultat.getBegrunnelseFritekst());
        dto.setBegrunnelser(new ArrayList<>());

        behandlingsresultat.getBehandlingsresultatBegrunnelser().stream()
            .map(BehandlingsresultatBegrunnelse::getKode)
            .forEach(dto.getBegrunnelser()::add);

        return dto;
    }
}
