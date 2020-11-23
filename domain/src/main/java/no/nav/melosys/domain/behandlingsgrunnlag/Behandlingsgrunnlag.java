package no.nav.melosys.domain.behandlingsgrunnlag;

import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.jpa.BehandlingsgrunnlagListener;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;

@Entity
@EntityListeners(BehandlingsgrunnlagListener.class)
@Table(name = "behandlingsgrunnlag")
public class Behandlingsgrunnlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Behandling behandling;

    @Column(name = "versjon", nullable = false)
    private String versjon;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private Instant registrertDato;

    @Column(name = "endret_dato", nullable = false)
    private Instant endretDato;

    @Column(name = "mottaksdato")
    private LocalDate mottaksdato;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Behandlingsgrunnlagtyper type;

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

    public Instant getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(Instant registrertDato) {
        this.registrertDato = registrertDato;
    }

    public Instant getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(Instant endretDato) {
        this.endretDato = endretDato;
    }

    public LocalDate getMottaksdato() {
        return mottaksdato;
    }

    public void setMottaksdato(LocalDate mottaksdato) {
        this.mottaksdato = mottaksdato;
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

    public Behandlingsgrunnlagtyper getType() {
        return type;
    }

    public void setType(Behandlingsgrunnlagtyper type) {
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

    public boolean erSøknad() {
        return erSøknadOmA1() || erSøknadFtrl();
    }

    public boolean erSøknadOmA1() {
        return type == Behandlingsgrunnlagtyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS
            || type == Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS;
    }

    public boolean erSøknadFtrl() {
        return type == Behandlingsgrunnlagtyper.SØKNAD_FOLKETRYGDEN;
    }

    public boolean erSed() {
        return this.type == Behandlingsgrunnlagtyper.SED;
    }
}
