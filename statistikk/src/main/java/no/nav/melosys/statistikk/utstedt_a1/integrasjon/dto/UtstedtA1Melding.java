package no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public class UtstedtA1Melding {
    private final String serienummer;
    private final String saksnummer;
    private final Long behandlingId;
    private final String aktorId;
    private final Lovvalgsbestemmelse artikkel;
    private final Periode periode;
    private final String utsendtTilLand;
    private final LocalDate datoUtstedelse;
    private final A1TypeUtstedelse typeUtstedelse;
    private final ZonedDateTime meldingOpprettetTidspunkt = ZonedDateTime.now();

    public UtstedtA1Melding(String saksnummer,
                            Long behandlingId,
                            String aktorId,
                            Lovvalgsbestemmelse artikkel,
                            Periode periode,
                            String utsendtTilLand,
                            LocalDate datoUtstedelse,
                            A1TypeUtstedelse typeUtstedelse) {
        this.serienummer = saksnummer + behandlingId;
        this.saksnummer = saksnummer;
        this.behandlingId = behandlingId;
        this.aktorId = aktorId;
        this.artikkel = artikkel;
        this.periode = periode;
        this.utsendtTilLand = utsendtTilLand;
        this.datoUtstedelse = datoUtstedelse;
        this.typeUtstedelse = typeUtstedelse;
    }

    public String getSerienummer() {
        return serienummer;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getAktorId() {
        return aktorId;
    }

    public Lovvalgsbestemmelse getArtikkel() {
        return artikkel;
    }

    public Periode getPeriode() {
        return periode;
    }

    public String getUtsendtTilLand() {
        return utsendtTilLand;
    }

    public LocalDate getDatoUtstedelse() {
        return datoUtstedelse;
    }

    public A1TypeUtstedelse getTypeUtstedelse() {
        return typeUtstedelse;
    }

    public ZonedDateTime getMeldingOpprettetTidspunkt() {
        return meldingOpprettetTidspunkt;
    }

    @Override
    public String toString() {
        return "UtstedtA1Melding{" +
            "serienummer='" + serienummer + '\'' +
            ", saksnummer='" + saksnummer + '\'' +
            ", behandlingId=" + behandlingId +
            ", artikkel=" + artikkel +
            ", periode=" + periode +
            ", utsendtTilLand='" + utsendtTilLand + '\'' +
            ", datoUtstedelse=" + datoUtstedelse +
            ", typeUtstedelse=" + typeUtstedelse +
            ", meldingOpprettetTidspunkt=" + meldingOpprettetTidspunkt +
            '}';
    }
}
