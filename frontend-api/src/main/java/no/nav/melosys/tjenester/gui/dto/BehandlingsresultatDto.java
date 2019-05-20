package no.nav.melosys.tjenester.gui.dto;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;

public class BehandlingsresultatDto {

    private final List<String> begrunnelser;
    private final String begrunnelseFritekst;

    private BehandlingsresultatDto(List<String> begrunnelser, String begrunnelseFritekst) {
        this.begrunnelser = begrunnelser;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public List<String> getBegrunnelser() {
        return begrunnelser;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public static BehandlingsresultatDto av(Behandlingsresultat behandlingsresultat) {
        BehandlingsresultatDto dto = new BehandlingsresultatDto(new ArrayList<>(), behandlingsresultat.getBegrunnelseFritekst());

        behandlingsresultat.getBehandlingsresultatBegrunnelser().stream()
            .map(BehandlingsresultatBegrunnelse::getKode)
            .forEach(dto.getBegrunnelser()::add);

        return dto;
    }
}
