package no.nav.melosys.tjenester.gui.dto;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public class BehandlingsresultatDto {
    private final String behandlingsresultatTypeKode;
    private final List<String> begrunnelseKoder;
    private final String begrunnelseFritekst;

    private BehandlingsresultatDto(Behandlingsresultattyper behandlingsresultatTypeKode, String begrunnelseFritekst) {
        this.behandlingsresultatTypeKode = behandlingsresultatTypeKode.getKode();
        this.begrunnelseKoder = new ArrayList<>();
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public static BehandlingsresultatDto av(Behandlingsresultat resultat) {
        BehandlingsresultatDto dto = new BehandlingsresultatDto(resultat.getType(), resultat.getBegrunnelseFritekst());

        resultat.getBehandlingsresultatBegrunnelser().stream()
            .map(BehandlingsresultatBegrunnelse::getKode)
            .forEach(dto.getBegrunnelseKoder()::add);

        return dto;
    }

    public String getBehandlingsresultatTypeKode() {
        return behandlingsresultatTypeKode;
    }

    public List<String> getBegrunnelseKoder() {
        return begrunnelseKoder;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }
}
