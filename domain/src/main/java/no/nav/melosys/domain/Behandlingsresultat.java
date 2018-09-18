package no.nav.melosys.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "behandlingsresultat")
public class Behandlingsresultat {

    // Populeres av Hibernate med behandling.id
    @Id
    private long id; 

    @MapsId
    @OneToOne(fetch=FetchType.EAGER, optional = false)
    @JoinColumn(name="behandling_id")
    private Behandling behandling;

    @Enumerated(EnumType.STRING)
    @Column(name = "behandlingsmaate", nullable = false, updatable = false)
    private Behandlingsmaate behandlingsmåte;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat_type", nullable = false, updatable = false)
    private BehandlingsresultatType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "fastsatt_av_land")
    private Landkoder fastsattAvLand;

    @Column(name = "har_vedtak", nullable = false)
    private boolean harVedtak;

    @Enumerated(EnumType.STRING)
    @Column(name = "henleggelse_grunn")
    private Henleggelsesgrunner henleggelsesgrunn;

    @Column(name = "henleggelse_fritekst")
    private String henleggelseFritekst;
    
    @Column(name = "vedtak_dato")
    private Instant vedtaksdato;

    @Column(name = "vedtak_klagefrist")
    private LocalDate vedtakKlagefrist;

    @OneToMany(mappedBy = "behandlingsresultat", fetch = FetchType.EAGER)
    private Set<LovvalgPeriode> lovvalgsperioder;

    public long getId() {
        return id;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public Behandlingsmaate getBehandlingsmåte() {
        return behandlingsmåte;
    }

    public void setBehandlingsmåte(Behandlingsmaate behandlingsmåte) {
        this.behandlingsmåte = behandlingsmåte;
    }

    public BehandlingsresultatType getType() {
        return type;
    }

    public void setType(BehandlingsresultatType type) {
        this.type = type;
    }

    public Landkoder getFastsattAvLand() {
        return fastsattAvLand;
    }

    public void setFastsattAvLand(Landkoder fastsattAvLand) {
        this.fastsattAvLand = fastsattAvLand;
    }

    public boolean isHarVedtak() {
        return harVedtak;
    }

    public void setHarVedtak(boolean harVedtak) {
        this.harVedtak = harVedtak;
    }

    public Henleggelsesgrunner getHenleggelsesgrunn() {
        return henleggelsesgrunn;
    }

    public void setHenleggelsesgrunn(Henleggelsesgrunner henleggelsesgrunn) {
        this.henleggelsesgrunn = henleggelsesgrunn;
    }

    public String getHenleggelseFritekst() {
        return henleggelseFritekst;
    }

    public void setHenleggelseFritekst(String henleggelseFritekst) {
        this.henleggelseFritekst = henleggelseFritekst;
    }

    public Instant getVedtaksdato() {
        return vedtaksdato;
    }

    public void setVedtaksdato(Instant vedtaksdato) {
        this.vedtaksdato = vedtaksdato;
    }

    public LocalDate getVedtakKlagefrist() {
        return vedtakKlagefrist;
    }

    public void setVedtakKlagefrist(LocalDate vedtakKlagefrist) {
        this.vedtakKlagefrist = vedtakKlagefrist;
    }

    public Set<LovvalgPeriode> getLovvalgsperioder() {
        return lovvalgsperioder;
    }

    public void setLovvalgsperioder(Set<LovvalgPeriode> lovvalgsperioder) {
        this.lovvalgsperioder = lovvalgsperioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Behandlingsresultat)) {
            return false;
        }
        Behandlingsresultat that = (Behandlingsresultat) o;
        return Objects.equals(this.type, that.type)
            && Objects.equals(this.behandlingsmåte, that.behandlingsmåte)
            && Objects.equals(this.behandling, that.behandling);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, behandlingsmåte, behandling);
    }
    
}
