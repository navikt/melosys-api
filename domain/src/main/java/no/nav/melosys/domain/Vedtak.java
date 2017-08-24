package no.nav.melosys.domain;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

//FIXME (farjam): Ikke revidert for v0

@Entity
@Table(name = "VEDTAK")
public class Vedtak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vedtak_id")
    private Long vedtaksId;

    @Column(name = "dato_fattet", nullable = false)
    private LocalDateTime datoFattet;

    @ManyToOne
    @JoinColumn(name = "resultat")
    private VedtaksResultatType vedtaksResultat;

    public Long getId() {
        return id;
    }

    public Long getVedtaksId() {
        return vedtaksId;
    }

    public void setVedtaksId(Long vedtaksId) {
        this.vedtaksId = vedtaksId;
    }

    public LocalDateTime getDatoFattet() {
        return datoFattet;
    }

    public void setDatoFattet(LocalDateTime datoFattet) {
        this.datoFattet = datoFattet;
    }

    public VedtaksResultatType getVedtaksResultat() {
        return vedtaksResultat;
    }

    public void setVedtaksResultat(VedtaksResultatType vedtaksResultat) {
        this.vedtaksResultat = vedtaksResultat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Vedtak vedtak = (Vedtak) o;
        return Objects.equals(vedtaksId, vedtak.getVedtaksId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(vedtaksId);
    }
}
