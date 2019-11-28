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
    private final String utfallRegistreringUnntak;
    private final String vedtakstype;

    private BehandlingsresultatDto(Behandlingsresultattyper behandlingsresultatTypeKode,
                                   String begrunnelseFritekst,
                                   String utfallRegistreringUnntak,
                                   String vedtakstype) {
        this.behandlingsresultatTypeKode = behandlingsresultatTypeKode.getKode();
        this.begrunnelseKoder = new ArrayList<>();
        this.begrunnelseFritekst = begrunnelseFritekst;
        this.utfallRegistreringUnntak = utfallRegistreringUnntak;
        this.vedtakstype = vedtakstype;
    }

    public static BehandlingsresultatDto av(Behandlingsresultat resultat) {
        BehandlingsresultatDto dto = new BehandlingsresultatDto(
            resultat.getType(),
            resultat.getBegrunnelseFritekst(),
            resultat.getUtfallRegistreringUnntak() != null ? resultat.getUtfallRegistreringUnntak().getKode() : null,
            resultat.getVedtakMetadata() != null ? resultat.getVedtakMetadata().getVedtakstype().getKode() : null    
        );

        resultat.getBehandlingsresultatBegrunnelser().stream()
            .map(BehandlingsresultatBegrunnelse::getKode)
            .forEach(dto.getBegrunnelseKoder()::add);

        if (resultat.getVedtakMetadata() != null && resultat.getVedtakMetadata().getRevurderBegrunnelse() != null) {
            dto.getBegrunnelseKoder().add(resultat.getVedtakMetadata().getRevurderBegrunnelse());
        }
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

    public String getUtfallRegistreringUnntak() {
        return utfallRegistreringUnntak;
    }

    public String getVedtakstype() {
        return vedtakstype;
    }
}
