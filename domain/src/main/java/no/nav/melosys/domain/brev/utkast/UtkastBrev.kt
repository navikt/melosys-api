package no.nav.melosys.domain.brev.utkast;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.exception.TekniskException;

@Entity
@Table(name = "utkast_brev")
public class UtkastBrev {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "behandling_id")
    private long behandlingID;

    private LocalDateTime lagringsdato;

    @Column(name = "lagret_av_saksbehandler")
    private String lagretAvSaksbehandler;

    @Lob
    private String brevbestillingUtkast;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public LocalDateTime getLagringsdato() {
        return lagringsdato;
    }

    public void setLagringsdato(LocalDateTime lagringsdato) {
        this.lagringsdato = lagringsdato;
    }

    public String getLagretAvSaksbehandler() {
        return lagretAvSaksbehandler;
    }

    public void setLagretAvSaksbehandler(String lagretAvSaksbehandler) {
        this.lagretAvSaksbehandler = lagretAvSaksbehandler;
    }

    public BrevbestillingUtkast getBrevbestillingUtkast() {
        try {
            var mapper = new ObjectMapper();
            return mapper.readValue(brevbestillingUtkast, BrevbestillingUtkast.class);
        } catch (JsonProcessingException e) {
            throw new TekniskException("Klarte ikke lese brevbestillingUtkast med ID %s".formatted(id), e);
        }
    }

    public void setBrevbestillingUtkast(BrevbestillingUtkast brevBestillingUtkast) {
        try {
            var mapper = new ObjectMapper();
            this.brevbestillingUtkast = mapper.writeValueAsString(brevBestillingUtkast);
        } catch (JsonProcessingException e) {
            throw new TekniskException("Klarte ikke skrive brevbestillingUtkast med ID %s".formatted(id), e);
        }
    }
}
