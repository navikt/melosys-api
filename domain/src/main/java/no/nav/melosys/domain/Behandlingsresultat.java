package no.nav.melosys.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static java.util.Optional.ofNullable;

@Entity
@Table(name = "behandlingsresultat")
@EntityListeners(AuditingEntityListener.class)
public class Behandlingsresultat extends RegistreringsInfo {
    // Populeres av Hibernate med behandling.id
    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "behandling_id")
    private Behandling behandling;

    @Enumerated(EnumType.STRING)
    @Column(name = "behandlingsmaate", nullable = false)
    private Behandlingsmaate behandlingsmåte;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat_type", nullable = false)
    private Behandlingsresultattyper type;

    @Enumerated(EnumType.STRING)
    @Column(name = "fastsatt_av_land")
    private Landkoder fastsattAvLand;

    @Column(name = "begrunnelse_fritekst")
    private String begrunnelseFritekst;

    @Column(name = "innledning_fritekst")
    private String innledningFritekst;

    @OneToOne(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private VedtakMetadata vedtakMetadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "utfall_registrering_unntak")
    private Utfallregistreringunntak utfallRegistreringUnntak;

    @Enumerated(EnumType.STRING)
    @Column(name = "utfall_utpeking")
    private Utfallregistreringunntak utfallUtpeking;

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Avklartefakta> avklartefakta = new HashSet<>(1);

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Lovvalgsperiode> lovvalgsperioder = new HashSet<>(1);

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Anmodningsperiode> anmodningsperioder = new HashSet<>(1);

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Utpekingsperiode> utpekingsperioder = new HashSet<>(1);

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Vilkaarsresultat> vilkaarsresultater = new HashSet<>(1);

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Kontrollresultat> kontrollresultater = new HashSet<>(1);

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<BehandlingsresultatBegrunnelse> behandlingsresultatBegrunnelser = new HashSet<>(1);

    @OneToOne(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private MedlemAvFolketrygden medlemAvFolketrygden;

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

    public Behandlingsmaate getBehandlingsmåte() {
        return behandlingsmåte;
    }

    public void setBehandlingsmåte(Behandlingsmaate behandlingsmåte) {
        this.behandlingsmåte = behandlingsmåte;
    }

    public Behandlingsresultattyper getType() {
        return type;
    }

    public void setType(Behandlingsresultattyper type) {
        this.type = type;
    }

    public Landkoder getFastsattAvLand() {
        return fastsattAvLand;
    }

    public void setFastsattAvLand(Landkoder fastsattAvLand) {
        this.fastsattAvLand = fastsattAvLand;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public void setInnledningFritekst(String innledningFritekst) {
        this.innledningFritekst = innledningFritekst;
    }

    public VedtakMetadata getVedtakMetadata() {
        return vedtakMetadata;
    }

    public void setVedtakMetadata(final VedtakMetadata vedtakMetadata) {
        this.vedtakMetadata = vedtakMetadata;
    }

    public Utfallregistreringunntak getUtfallRegistreringUnntak() {
        return utfallRegistreringUnntak;
    }

    public void setUtfallRegistreringUnntak(Utfallregistreringunntak utfallRegistreringUnntak) {
        this.utfallRegistreringUnntak = utfallRegistreringUnntak;
    }

    public Utfallregistreringunntak getUtfallUtpeking() {
        return utfallUtpeking;
    }

    public void setUtfallUtpeking(Utfallregistreringunntak utfallUtpeking) {
        this.utfallUtpeking = utfallUtpeking;
    }

    public Set<Lovvalgsperiode> getLovvalgsperioder() {
        return lovvalgsperioder;
    }

    public void setLovvalgsperioder(Set<Lovvalgsperiode> lovvalgsperioder) {
        this.lovvalgsperioder = lovvalgsperioder;
    }

    public Set<Anmodningsperiode> getAnmodningsperioder() {
        return anmodningsperioder;
    }

    public void setAnmodningsperioder(Set<Anmodningsperiode> anmodningsperioder) {
        this.anmodningsperioder = anmodningsperioder;
    }

    public Set<Utpekingsperiode> getUtpekingsperioder() {
        return utpekingsperioder;
    }

    public void setUtpekingsperioder(Set<Utpekingsperiode> utpekingsperioder) {
        this.utpekingsperioder = utpekingsperioder;
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

    public Set<BehandlingsresultatBegrunnelse> getBehandlingsresultatBegrunnelser() {
        return behandlingsresultatBegrunnelser;
    }

    public void setBehandlingsresultatBegrunnelser(Set<BehandlingsresultatBegrunnelse> behandlingsresultatBegrunnelser) {
        this.behandlingsresultatBegrunnelser = behandlingsresultatBegrunnelser;
    }

    public Set<Kontrollresultat> getKontrollresultater() {
        return kontrollresultater;
    }

    public void setKontrollresultater(Set<Kontrollresultat> kontrollresultater) {
        this.kontrollresultater = kontrollresultater;
    }

    public MedlemAvFolketrygden getMedlemAvFolketrygden() {
        return medlemAvFolketrygden;
    }

    public Optional<MedlemAvFolketrygden> finnMedlemAvFolketrygden() {
        return ofNullable(getMedlemAvFolketrygden());
    }

    public void setMedlemAvFolketrygden(MedlemAvFolketrygden medlemAvFolketrygden) {
        this.medlemAvFolketrygden = medlemAvFolketrygden;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Behandlingsresultat that)) {
            return false;
        }
        return Objects.equals(this.type, that.type)
            && Objects.equals(this.behandling, that.behandling);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, behandling);
    }

    public boolean erAvslag() {
        return erAvslagManglendeOpplysninger() || (type == Behandlingsresultattyper.AVSLAG_SØKNAD)
            || (type == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND && hentLovvalgsperiode().erAvslått());
    }

    public boolean erAvslagManglendeOpplysninger() {
        return type == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL;
    }

    public boolean erAnmodningOmUnntak() {
        return type == Behandlingsresultattyper.ANMODNING_OM_UNNTAK;
    }

    public boolean erInnvilgelse() {
        if (type == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            || type == Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND) {
            return finnLovvalgsperiode().filter(Lovvalgsperiode::erInnvilget).isPresent();
        }
        return false;
    }

    public boolean erInnvilgelseFlereLand() {
        return erInnvilgelse() && finnLovvalgsperiode().stream().anyMatch(PeriodeOmLovvalg::erArtikkel13);
    }

    public boolean erUtpeking() {
        if (type == Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND) {
            return !utpekingsperioder.isEmpty();
        } else {
            return false;
        }
    }

    public boolean erIkkeArtikkel16MedSendtAnmodningOmUnntak() {
        return !erArtikkel16MedSendtAnmodningOmUnntak();
    }

    public boolean erArtikkel16MedSendtAnmodningOmUnntak() {
        return anmodningsperioder.stream().anyMatch(Anmodningsperiode::erSendtUtland);
    }

    public boolean erArt16EtterUtlandMedRegistrertSvar() {
        return finnAnmodningsperiode()
            .filter(Anmodningsperiode::harRegistrertSvar)
            .isPresent();
    }

    public boolean harLovvalgsperiodeMedBestemmelse(LovvalgBestemmelse lovvalgBestemmelse) {
        return finnLovvalgsperiode()
            .filter(lovvalgsperiode -> lovvalgsperiode.getBestemmelse() == lovvalgBestemmelse)
            .isPresent();
    }

    public boolean erGodkjenningEllerInnvilgelseArt13() {
        return (erInnvilgelse() || erGodkjenningRegistreringUnntak())
            && finnLovvalgsperiode().stream().anyMatch(PeriodeOmLovvalg::erArtikkel13);
    }

    public boolean harPeriodeOmLovvalg() {
        return !lovvalgsperioder.isEmpty() || !anmodningsperioder.isEmpty() || !utpekingsperioder.isEmpty();
    }

    public PeriodeOmLovvalg hentValidertPeriodeOmLovvalg() {
        if (!lovvalgsperioder.isEmpty()) {
            return hentLovvalgsperiode();
        } else if (!anmodningsperioder.isEmpty()) {
            return hentAnmodningsperiode();
        } else if (!utpekingsperioder.isEmpty()) {
            return hentValidertUtpekingsperiode();
        }

        throw new NoSuchElementException("Ingen periode om lovvalg finnes for behandling " + id);
    }

    public Optional<PeriodeOmLovvalg> finnValidertPeriodeOmLovvalg() {
        var lovvalgsperiodeOptional = finnLovvalgsperiode();
        Optional<? extends PeriodeOmLovvalg> periodeOmLovvalgOptional = lovvalgsperiodeOptional.isPresent() ?
            lovvalgsperiodeOptional : finnAnmodningsperiode();
        return periodeOmLovvalgOptional.map(PeriodeOmLovvalg.class::cast);
    }

    public Lovvalgsperiode hentLovvalgsperiode() {
        return finnLovvalgsperiode()
            .orElseThrow(() -> new NoSuchElementException("Ingen lovvalgsperiode finnes for behandlingsresultat " + id));
    }

    public Optional<Lovvalgsperiode> finnLovvalgsperiode() {
        if (lovvalgsperioder.size() > 1) {
            throw new UnsupportedOperationException("Flere enn en lovvalgsperiode er ikke støttet");
        }
        return lovvalgsperioder.stream().findFirst();
    }

    public Anmodningsperiode hentAnmodningsperiode() {
        return finnAnmodningsperiode()
            .orElseThrow(() -> new NoSuchElementException("Ingen anmodningsperioder finnes for behandlingsresultat " + id));
    }

    public Optional<Anmodningsperiode> finnAnmodningsperiode() {
        if (anmodningsperioder.size() > 1) {
            throw new FunksjonellException("Flere enn en anmodningsperiode er ikke støttet");
        }
        return anmodningsperioder.stream().findFirst();
    }

    public Utpekingsperiode hentValidertUtpekingsperiode() {
        return finnValidertUtpekingsperiode()
            .orElseThrow(() -> new NoSuchElementException("Ingen utpekingsperioder finnes for behandlingsresultat " + id));
    }

    public Optional<Utpekingsperiode> finnValidertUtpekingsperiode() {
        if (utpekingsperioder.size() > 1) {
            throw new UnsupportedOperationException("Flere enn en utpekingsperiode er ikke støttet");
        }
        return utpekingsperioder.stream().findFirst();
    }

    public Medlemskapsperiode hentValidertMedlemskapsPeriode() {
        return finnValidertMedlemskapsPeriode()
            .orElseThrow(() -> new NoSuchElementException("Ingen medlemskapsPerioder finnes for behandlingsresultat " + id));
    }

    public Optional<Medlemskapsperiode> finnValidertMedlemskapsPeriode() {
        Collection<Medlemskapsperiode> medlemskapsPerioder = medlemAvFolketrygden.getMedlemskapsperioder();
//        if (medlemskapsPerioder.size() > 1) {
//            throw new UnsupportedOperationException("Flere enn en medlemskapsPerioder er ikke støttet");
//        }

        return medlemskapsPerioder.stream().findFirst();
    }

    public Set<VilkaarBegrunnelse> hentVilkaarbegrunnelser(Vilkaar vilkaarType) {
        return getVilkaarsresultater().stream()
            .filter(vr -> vr.getVilkaar() == vilkaarType)
            .flatMap(vr -> vr.getBegrunnelser().stream())
            .collect(Collectors.toSet());
    }

    public boolean manglerVilkår(Vilkaar vilkår) {
        return vilkaarsresultater.stream().noneMatch(v -> v.getVilkaar() == vilkår);
    }

    public boolean oppfyllerVilkår(Collection<Vilkaar> vilkår) {
        return vilkår.stream().allMatch(this::oppfyllerVilkår);
    }

    public boolean oppfyllerVilkår(Vilkaar vilkår) {
        return vilkaarsresultater.stream()
            .anyMatch(v -> v.getVilkaar() == vilkår && v.isOppfylt());
    }

    public boolean erAutomatisert() {
        return behandlingsmåte == Behandlingsmaate.AUTOMATISERT
            || behandlingsmåte == Behandlingsmaate.DELVIS_AUTOMATISERT;
    }

    public boolean erInnvilgetArbeidPåSkipOmfattetAvArbeidsland() {
        return finnLovvalgsperiode().stream()
            .anyMatch(l -> l.erInnvilget()
                && l.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
                && l.getTilleggsbestemmelse() == Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
    }

    public boolean erRegistrertUnntak() {
        return type == Behandlingsresultattyper.REGISTRERT_UNNTAK;
    }

    public boolean erGodkjenningRegistreringUnntak() {
        return erRegistrertUnntak() && utfallRegistreringUnntak == Utfallregistreringunntak.GODKJENT;
    }

    public boolean a1Produseres() {
        return erInnvilgelse() && !erUtpeking() && harVedtak();
    }

    public boolean utlandSkalVarslesOmVedtak() {
        return harVedtak()
            && ((erInnvilgelse() && !harLovvalgsperiodeMedBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1))
            || erInnvilgelseFlereLand()
            || erUtpeking());
    }

    public void settVedtakMetadata(Vedtakstyper vedtakstype,
                                   String nyVurderingBakgrunn,
                                   LocalDate klagefrist) {
        if (vedtakMetadata == null) {
            vedtakMetadata = new VedtakMetadata();
            vedtakMetadata.setBehandlingsresultat(this);
            setVedtakMetadata(vedtakMetadata);
        } else {
            throw new UnsupportedOperationException("Trenger vi å oppdatere et vedtak?");
        }

        vedtakMetadata.setVedtakstype(vedtakstype);
        vedtakMetadata.setVedtaksdato(Instant.now());
        vedtakMetadata.setNyVurderingBakgrunn(nyVurderingBakgrunn);
        vedtakMetadata.setVedtakKlagefrist(klagefrist);
    }

    public boolean harVedtak() {
        return vedtakMetadata != null;
    }

    public boolean erUtpekingNorgeAvvist() {
        return type == Behandlingsresultattyper.UTPEKING_NORGE_AVVIST;
    }

    @Override
    public String toString() {
        return "Behandlingsresultat{" +
            "id=" + id +
            ", type=" + type +
            '}';
    }
}
