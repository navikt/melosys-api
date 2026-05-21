package no.nav.melosys.domain.tekstblokk;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import no.nav.melosys.domain.RegistreringsInfo;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "TEKSTBLOKK")
@EntityListeners(AuditingEntityListener.class)
public class Tekstblokk extends RegistreringsInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tittel", nullable = false)
    private String tittel;

    @Lob
    @Column(name = "innhold", nullable = false)
    private String innhold;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TekstblokkType type;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "TEKSTBLOKK_TAG", joinColumns = @JoinColumn(name = "tekstblokk_id"))
    @Column(name = "tag", nullable = false)
    @BatchSize(size = 50)
    private Set<String> tags = new HashSet<>();

    public Long getId() {
        return id;
    }

    public String getTittel() {
        return tittel;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }

    public String getInnhold() {
        return innhold;
    }

    public void setInnhold(String innhold) {
        this.innhold = innhold;
    }

    public TekstblokkType getType() {
        return type;
    }

    public void setType(TekstblokkType type) {
        this.type = type;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
