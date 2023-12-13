package no.nav.melosys.domain;

import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.TekniskException;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

@Entity
@Table(name = "aktoer")
@Audited
@AuditOverride(forClass = RegistreringsInfo.class)
@EntityListeners(AuditingEntityListener.class)
public class Aktoer extends RegistreringsInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "saksnummer", updatable = false)
    @Audited(targetAuditMode = NOT_AUDITED)
    private Fagsak fagsak;

    @Column(name = "person_ident")
    private String personIdent;

    @Column(name = "aktoer_id", updatable = false)
    private String aktørId;

    @Column(name = "eu_eos_institusjon_id", updatable = false)
    private String institusjonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trygdemyndighet_land", updatable = false)
    private Land_iso2 trygdemyndighetLand;

    @Column(name = "orgnr")
    private String orgnr;

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle", nullable = false, updatable = false)
    private Aktoersroller rolle;

    @Column(name = "utenlandsk_person_id")
    private String utenlandskPersonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "representerer")
    private Representerer representerer;

    @OneToMany(mappedBy = "aktoer", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Fullmakt> fullmakter = new HashSet<>(1);

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Fagsak getFagsak() {
        return fagsak;
    }

    public void setFagsak(Fagsak fagsak) {
        this.fagsak = fagsak;
    }

    public String getPersonIdent() {
        return personIdent;
    }

    public void setPersonIdent(String ident) {
        this.personIdent = ident;
    }

    public String getAktørId() {
        return aktørId;
    }

    public void setAktørId(String aktørId) {
        this.aktørId = aktørId;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public Aktoersroller getRolle() {
        return rolle;
    }

    public void setRolle(Aktoersroller rolle) {
        this.rolle = rolle;
    }

    public String getInstitusjonId() {
        return institusjonId;
    }

    public void setInstitusjonId(String institusjonId) {
        this.institusjonId = institusjonId;
    }

    public String getUtenlandskPersonId() {
        return utenlandskPersonId;
    }

    public void setUtenlandskPersonId(String utenlandskPersonId) {
        this.utenlandskPersonId = utenlandskPersonId;
    }

    public Representerer getRepresenterer() {
        return representerer;
    }

    public void setRepresenterer(Representerer representerer) {
        this.representerer = representerer;
    }

    public Set<Fullmakt> getFullmakter() {
        return fullmakter;
    }

    public Set<Fullmaktstype> getFullmaktstyper() {
        return fullmakter != null ? fullmakter.stream().map(Fullmakt::getType).collect(Collectors.toSet()) : Collections.emptySet();
    }

    public void setFullmakter(Set<Fullmakt> fullmakter) {
        this.fullmakter.clear();
        this.fullmakter.addAll(fullmakter);
    }

    public void setFullmaktstyper(Collection<Fullmaktstype> fullmaktstyper) {
        setFullmakter(fullmaktstyper.stream().map((fullmaktstype -> {
            var fullmakt = new Fullmakt();
            fullmakt.setType(fullmaktstype);
            fullmakt.setAktoer(this);
            return fullmakt;
        })).collect(Collectors.toSet()));
    }

    public void setFullmaktstype(Fullmaktstype fullmaktstype) {
        setFullmaktstyper(Set.of(fullmaktstype));
    }

    public boolean erPerson() {
        return switch (rolle) {
            case BRUKER -> true;
            case REPRESENTANT, FULLMEKTIG -> personIdent != null;
            default -> false;
        };
    }

    public boolean erOrganisasjon() {
        return switch (rolle) {
            case BRUKER -> false;
            case REPRESENTANT, FULLMEKTIG -> orgnr != null;
            default -> true;
        };
    }

    public boolean erUtenlandskMyndighet() {
        return rolle == Aktoersroller.TRYGDEMYNDIGHET && (institusjonId != null || trygdemyndighetLand != null);
    }

    public Land_iso2 getTrygdemyndighetLand() {
        return trygdemyndighetLand;
    }

    public void setTrygdemyndighetLand(Land_iso2 trygdemyndighetLandkode) {
        this.trygdemyndighetLand = trygdemyndighetLandkode;
    }

    public Land_iso2 hentMyndighetLandkode() {
        if (erUtenlandskMyndighet()) {
            return institusjonId != null ? UtenlandskMyndighet.konverterInstitusjonIdTilLandkode(institusjonId) : trygdemyndighetLand;
        }
        throw new TekniskException("Aktør " + id + " er ikke en utenlandsk myndighet");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Aktoer that)) {
            return false;
        }
        return Objects.equals(this.fagsak, that.fagsak)
            && Objects.equals(this.aktørId, that.aktørId)
            && Objects.equals(this.orgnr, that.orgnr)
            && Objects.equals(this.institusjonId, that.institusjonId)
            && Objects.equals(this.trygdemyndighetLand, that.trygdemyndighetLand)
            && Objects.equals(this.utenlandskPersonId, that.utenlandskPersonId)
            && Objects.equals(this.rolle, that.rolle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsak, aktørId, orgnr, utenlandskPersonId, rolle, institusjonId);
    }
}
