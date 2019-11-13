package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public class FattVedtakDto {
    private Behandlingsresultattyper behandlingsresultatTypeKode;
    private String mottakerinstitusjon;

    public Behandlingsresultattyper getBehandlingsresultatTypeKode() {
        return behandlingsresultatTypeKode;
    }

    public void setBehandlingsresultatTypeKode(Behandlingsresultattyper behandlingsresultatTypeKode) {
        this.behandlingsresultatTypeKode = behandlingsresultatTypeKode;
    }

    public String getMottakerinstitusjon() {
        return mottakerinstitusjon;
    }

    public void setMottakerinstitusjon(String mottakerinstitusjon) {
        this.mottakerinstitusjon = mottakerinstitusjon;
    }
}
