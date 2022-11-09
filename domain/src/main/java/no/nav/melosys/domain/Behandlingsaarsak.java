package no.nav.melosys.domain;

import java.time.LocalDate;
import javax.annotation.Nonnull;
import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;

@Entity
@Table(name = "behandlingsaarsak")
public class Behandlingsaarsak {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    private Behandling behandling;

    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak_type", nullable = false)
    private Behandlingsaarsaktyper type;

    @Column(name = "aarsak_fritekst")
    private String fritekst;

    @Column(name = "mottak_dato", nullable = false)
    private LocalDate mottaksdato;

    public Behandlingsaarsak() {
        // JPA
    }

    public Behandlingsaarsak(@Nonnull Behandlingsaarsaktyper type, @Nonnull LocalDate mottaksdato) {
        this.type = type;
        this.mottaksdato = mottaksdato;
    }

    public Behandlingsaarsak(@Nonnull Behandlingsaarsaktyper type, String fritekst, @Nonnull LocalDate mottaksdato) {
        this.type = type;
        this.fritekst = fritekst;
        this.mottaksdato = mottaksdato;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public Behandlingsaarsaktyper getType() {
        return type;
    }

    public void setType(Behandlingsaarsaktyper type) {
        this.type = type;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(String fritekst) {
        this.fritekst = fritekst;
    }

    public LocalDate getMottaksdato() {
        return mottaksdato;
    }

    public void setMottaksdato(LocalDate mottaksdato) {
        this.mottaksdato = mottaksdato;
    }
}
