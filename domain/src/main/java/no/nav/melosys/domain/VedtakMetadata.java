package no.nav.melosys.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "vedtak_metadata")
@EntityListeners(AuditingEntityListener.class)
public class VedtakMetadata extends RegistreringsInfo {
    @Id
    private Long id;

    @Column(name = "vedtak_dato")
    private Instant vedtaksdato;

    @Column(name = "vedtak_klagefrist")
    private LocalDate vedtakKlagefrist;

    @Column(name = "vedtak_type")
    private Vedtakstyper vedtakstype;
    
    @Column(name = "revurder_begrunnelse")
    private String revurderBegrunnelse;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "behandlingsresultat_id")
    private Behandlingsresultat behandlingsresultat;
    
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Instant getVedtaksdato() {
        return vedtaksdato;
    }

    public void setVedtaksdato(final Instant vedtaksdato) {
        this.vedtaksdato = vedtaksdato;
    }

    public LocalDate getVedtakKlagefrist() {
        return vedtakKlagefrist;
    }

    public void setVedtakKlagefrist(final LocalDate vedtakKlagefrist) {
        this.vedtakKlagefrist = vedtakKlagefrist;
    }

    public Vedtakstyper getVedtakstype() {
        return vedtakstype;
    }

    public void setVedtakstype(final Vedtakstyper vedtakstype) {
        this.vedtakstype = vedtakstype;
    }

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public void setBehandlingsresultat(final Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    public String getRevurderBegrunnelse() {
        return revurderBegrunnelse;
    }

    public void setRevurderBegrunnelse(String revurderBegrunnelse) {
        this.revurderBegrunnelse = revurderBegrunnelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VedtakMetadata)) {
            return false;
        }
        VedtakMetadata that = (VedtakMetadata) o;
        return Objects.equals(this.vedtakstype, that.vedtakstype)
            && Objects.equals(this.behandlingsresultat.getId(), that.behandlingsresultat.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(vedtakstype, behandlingsresultat.getId());
    }


}
