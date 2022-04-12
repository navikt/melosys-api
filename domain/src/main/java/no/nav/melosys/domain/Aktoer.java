package no.nav.melosys.domain;

import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.TekniskException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "aktoer")
@EntityListeners(AuditingEntityListener.class)
public class Aktoer extends RegistreringsInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "saksnummer", updatable = false)
    private Fagsak fagsak;

    @Column(name = "person_ident")
    private String personIdent;

    @Column(name = "aktoer_id", updatable = false)
    private String aktørId;

    @Column(name = "institusjon_id", updatable = false)
    private String institusjonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trygdemyndighet_land", updatable = false)
    private Landkoder trygdemyndighetLand;

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

    public boolean erPerson() {
        return switch (rolle) {
            case BRUKER -> true;
            case REPRESENTANT -> personIdent != null;
            default -> false;
        };
    }

    public boolean erOrganisasjon() {
        return switch (rolle) {
            case BRUKER -> false;
            case REPRESENTANT -> orgnr != null;
            default -> true;
        };
    }

    public boolean erUtenlandskMyndighet() {
        return rolle == Aktoersroller.TRYGDEMYNDIGHET && institusjonId != null && trygdemyndighetLand != null;
    }

    public boolean erBruker() {
        return Aktoersroller.BRUKER.equals(rolle);
    }

    public Landkoder getTrygdemyndighetLand() {
        return trygdemyndighetLand;
    }

    public void setTrygdemyndighetLand(Landkoder trygdemyndighetLandkode) {
        this.trygdemyndighetLand = trygdemyndighetLandkode;
    }

    public Landkoder hentMyndighetLandkode() {
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
