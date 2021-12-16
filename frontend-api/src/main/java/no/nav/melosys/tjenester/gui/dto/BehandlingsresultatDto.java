package no.nav.melosys.tjenester.gui.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.Kontrollresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public class BehandlingsresultatDto {
    private final String behandlingsresultatTypeKode;
    private final List<String> begrunnelseKoder;
    private final String begrunnelseFritekst;
    private final String innledningFritekst;
    private final String utfallRegistreringUnntak;
    private final String utfallUtpeking;
    private final String vedtakstype;
    private final List<String> kontrollresultatBegrunnelseKoder;

    private BehandlingsresultatDto(Behandlingsresultattyper behandlingsresultatTypeKode,
                                   String begrunnelseFritekst,
                                   String innledningFritekst,
                                   String utfallRegistreringUnntak,
                                   String utfallUtpeking,
                                   String vedtakstype,
                                   List<String> kontrollresultatBegrunnelseKoder) {
        this.behandlingsresultatTypeKode = behandlingsresultatTypeKode.getKode();
        this.innledningFritekst = innledningFritekst;
        this.utfallUtpeking = utfallUtpeking;
        this.begrunnelseKoder = new ArrayList<>();
        this.begrunnelseFritekst = begrunnelseFritekst;
        this.utfallRegistreringUnntak = utfallRegistreringUnntak;
        this.vedtakstype = vedtakstype;
        this.kontrollresultatBegrunnelseKoder = kontrollresultatBegrunnelseKoder;
    }

    public static BehandlingsresultatDto av(Behandlingsresultat resultat) {
        BehandlingsresultatDto dto = new BehandlingsresultatDto(
            resultat.getType(),
            resultat.getBegrunnelseFritekst(),
            resultat.getInnledningFritekst(),
            resultat.getUtfallRegistreringUnntak() != null ? resultat.getUtfallRegistreringUnntak().getKode() : null,
            resultat.getUtfallUtpeking() != null ? resultat.getUtfallUtpeking().getKode() : null,
            resultat.getVedtakMetadata() != null ? resultat.getVedtakMetadata().getVedtakstype().getKode() : null,
            resultat.getKontrollresultater().stream().map(Kontrollresultat::getBegrunnelse).map(Kontroll_begrunnelser::getKode).collect(Collectors.toList())
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

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getUtfallRegistreringUnntak() {
        return utfallRegistreringUnntak;
    }

    public String getUtfallUtpeking() {
        return utfallUtpeking;
    }

    public String getVedtakstype() {
        return vedtakstype;
    }

    public List<String> getKontrollresultatBegrunnelseKoder() {
        return kontrollresultatBegrunnelseKoder;
    }
}
