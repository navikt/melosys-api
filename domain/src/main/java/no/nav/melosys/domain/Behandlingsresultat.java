package no.nav.melosys.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "behandlingsresultat")
@EntityListeners(AuditingEntityListener.class)
public class Behandlingsresultat extends RegistreringsInfo {

    // Populeres av Hibernate med behandling.id
    @Id
    private long id; 

    @MapsId
    @OneToOne(fetch=FetchType.EAGER, optional = false)
    @JoinColumn(name="behandling_id")
    private Behandling behandling;

    @Enumerated(EnumType.STRING)
    @Column(name = "behandlingsmaate", nullable = false)
    private Behandlingsmaate behandlingsmåte;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat_type", nullable = false)
    private BehandlingsresultatType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "fastsatt_av_land")
    private Landkoder fastsattAvLand;

    @Enumerated(EnumType.STRING)
    @Column(name = "henleggelse_grunn")
    private Henleggelsesgrunner henleggelsesgrunn;

    @Column(name = "henleggelse_fritekst")
    private String henleggelseFritekst;
    
    @Column(name = "vedtak_dato")
    private Instant vedtaksdato;

    @Column(name = "vedtak_klagefrist")
    private LocalDate vedtakKlagefrist;

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Avklartefakta> avklartefakta;

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Lovvalgsperiode> lovvalgsperioder;

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Vilkaarsresultat> vilkaarsresultater;

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

    public Henleggelsesgrunner getHenleggelsesgrunn() {
        return henleggelsesgrunn;
    }

    public void setHenleggelsesgrunn(Henleggelsesgrunner henleggelsesgrunn) {
        this.henleggelsesgrunn = henleggelsesgrunn;
    }

    /**
     * Returnerer henleggelsesfritekst bare hvis hendelsesgrunn er ANNET. Ellers returneres NULL
     */
    public String hentHenleggelseFritekstHvisHenleggelsesgrunnANNET() {
        return Henleggelsesgrunner.ANNET == henleggelsesgrunn ? henleggelseFritekst : null;
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

    public Set<Lovvalgsperiode> getLovvalgsperioder() {
        return lovvalgsperioder;
    }

    public void setLovvalgsperioder(Set<Lovvalgsperiode> lovvalgsperioder) {
        this.lovvalgsperioder = lovvalgsperioder;
    }

    public Set<Vilkaarsresultat> getVilkaarsresultater() {
        return vilkaarsresultater;
    }

    public void setVilkaarsresultater(Set<Vilkaarsresultat> vilkaarsresultater) {
        this.vilkaarsresultater = vilkaarsresultater;
    }

    public Set<Avklartefakta> getAvklartefakta() {
        return avklartefakta;
    }

    public void setAvklartefakta(Set<Avklartefakta> avklartefakta) {
        this.avklartefakta = avklartefakta;
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
            && Objects.equals(this.behandling, that.behandling);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, behandling);
    }
    
}
