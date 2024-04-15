package no.nav.melosys.domain.oppgave;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "oppgave_tilbakelegging")
public class OppgaveTilbakelegging {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oppgave_id", nullable = false, updatable = false)
    private String oppgaveId;

    @Column(name = "saksbehandler_id", nullable = false, updatable = false)
    private String saksbehandlerId;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    public String getOppgaveId() {
        return oppgaveId;
    }

    public void setOppgaveId(String oppgaveId) {
        this.oppgaveId = oppgaveId;
    }

    public String getSaksbehandlerId() {
        return saksbehandlerId;
    }

    public void setSaksbehandlerId(String saksbehandlerId) {
        this.saksbehandlerId = saksbehandlerId;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }
}
