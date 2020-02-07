package no.nav.melosys.domain.grunnlag;

import javax.persistence.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.jpa.BehandlingsgrunnlagListener;

@Entity
@EntityListeners(BehandlingsgrunnlagListener.class)
@Table(name = "behandlingsgrunnlag")
public class Behandlingsgrunnlag {

    @Id
    private Long id;

    @MapsId
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    private Behandling behandling;

    @Column(name = "versjon", nullable = false)
    private String versjon;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BehandlingsGrunnlagType type;

    @Lob
    @Column(name = "original_data", updatable = false)
    private String originalData;

    @Lob
    @Column(name = "data")
    private String jsonData;

    @Transient
    private BehandlingsgrunnlagData behandlingsgrunnlagdata;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersjon() {
        return versjon;
    }

    public void setVersjon(String versjon) {
        this.versjon = versjon;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public String getOriginalData() {
        return originalData;
    }

    public void setOriginalData(String originalData) {
        this.originalData = originalData;
    }

    public BehandlingsGrunnlagType getType() {
        return type;
    }

    public void setType(BehandlingsGrunnlagType type) {
        this.type = type;
    }

    public String getJsonData() {
        return jsonData;
    }

    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }

    public BehandlingsgrunnlagData getBehandlingsgrunnlagdata() {
        return behandlingsgrunnlagdata;
    }

    public void setBehandlingsgrunnlagdata(BehandlingsgrunnlagData behandlingsgrunnlagdata) {
        this.behandlingsgrunnlagdata = behandlingsgrunnlagdata;
    }
}
