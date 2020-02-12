package no.nav.melosys.tjenester.gui.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.Registerkontroll;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public class BehandlingsresultatDto {
    private final String behandlingsresultatTypeKode;
    private final List<String> begrunnelseKoder;
    private final String begrunnelseFritekst;
    private final String utfallRegistreringUnntak;
    private final String vedtakstype;
    private final List<String> kontrollBegrunnelseKoder;

    private BehandlingsresultatDto(Behandlingsresultattyper behandlingsresultatTypeKode,
                                   String begrunnelseFritekst,
                                   String utfallRegistreringUnntak,
                                   String vedtakstype,
                                   List<String> kontrollBegrunnelseKoder) {
        this.behandlingsresultatTypeKode = behandlingsresultatTypeKode.getKode();
        this.begrunnelseKoder = new ArrayList<>();
        this.begrunnelseFritekst = begrunnelseFritekst;
        this.utfallRegistreringUnntak = utfallRegistreringUnntak;
        this.vedtakstype = vedtakstype;
        this.kontrollBegrunnelseKoder = kontrollBegrunnelseKoder;
    }

    public static BehandlingsresultatDto av(Behandlingsresultat resultat) {
        BehandlingsresultatDto dto = new BehandlingsresultatDto(
            resultat.getType(),
            resultat.getBegrunnelseFritekst(),
            resultat.getUtfallRegistreringUnntak() != null ? resultat.getUtfallRegistreringUnntak().getKode() : null,
            resultat.getVedtakMetadata() != null ? resultat.getVedtakMetadata().getVedtakstype().getKode() : null,
            resultat.getRegisterkontroller().stream().map(Registerkontroll::getBegrunnelse).map(Kontroll_begrunnelser::getKode).collect(Collectors.toList())
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

    public List<String> getKontrollBegrunnelseKoder() {
        return kontrollBegrunnelseKoder;
    }
}
