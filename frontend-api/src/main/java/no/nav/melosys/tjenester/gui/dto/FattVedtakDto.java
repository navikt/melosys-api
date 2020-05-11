package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public class FattVedtakDto {
    private Behandlingsresultattyper behandlingsresultatTypeKode;
    private String fritekst;
    private String fritekstSed;
    private Set<String> mottakerinstitusjoner;
    private Vedtakstyper vedtakstype;
    private String revurderBegrunnelse;

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

    public String getFritekstSed() {
        return fritekstSed;
    }

    public void setFritekstSed(String fritekstSed) {
        this.fritekstSed = fritekstSed;
    }

    public Set<String> getMottakerinstitusjoner() {
        return mottakerinstitusjoner;
    }

    public void setMottakerinstitusjoner(Set<String> mottakerinstitusjoner) {
        this.mottakerinstitusjoner = mottakerinstitusjoner;
    }

    public Vedtakstyper getVedtakstype() {
        return vedtakstype;
    }

    public void setVedtakstype(Vedtakstyper vedtakstype) {
        this.vedtakstype = vedtakstype;
    }

    public String getRevurderBegrunnelse() {
        return revurderBegrunnelse;
    }

    public void setRevurderBegrunnelse(String revurderBegrunnelse) {
        this.revurderBegrunnelse = revurderBegrunnelse;
    }
}
