package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public class FattVedtakDto {
    private Behandlingsresultattyper behandlingsresultatTypeKode;
    private String fritekst;

    public Behandlingsresultattyper getBehandlingsresultatTypeKode() {
        return behandlingsresultatTypeKode;
    }

    public void setBehandlingsresultatTypeKode(Behandlingsresultattyper behandlingsresultatTypeKode) {
        this.behandlingsresultatTypeKode = behandlingsresultatTypeKode;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(final String fritekst) {
        this.fritekst = fritekst;
    }
}
