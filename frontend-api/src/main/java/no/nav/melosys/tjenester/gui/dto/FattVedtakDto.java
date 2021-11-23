package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = FattEosVedtakDto.class)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = FattEosVedtakDto.class),
        @JsonSubTypes.Type(value = FattTrygdeavtaleEllerFtrlVedtakDto.class)
    }
)
public abstract class FattVedtakDto {
    private Behandlingsresultattyper behandlingsresultatTypeKode;
    private Vedtakstyper vedtakstype;

    public Behandlingsresultattyper getBehandlingsresultatTypeKode() {
        return behandlingsresultatTypeKode;
    }

    public void setBehandlingsresultatTypeKode(Behandlingsresultattyper behandlingsresultatTypeKode) {
        this.behandlingsresultatTypeKode = behandlingsresultatTypeKode;
    }

    public Vedtakstyper getVedtakstype() {
        return vedtakstype;
    }

    public void setVedtakstype(Vedtakstyper vedtakstype) {
        this.vedtakstype = vedtakstype;
    }
}
