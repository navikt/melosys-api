package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.TekniskException;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class Mangelbrev extends DokgenDto {

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoVedtatt;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoInnsendingsfrist;

    private final Sakstyper sakstype;
    private final Behandlingstyper behandlingstype;
    private final String saksbehandlerNavn;
    private final String fritekstMangelinfo;
    private final String fritekstMottaksinfo;

    protected Mangelbrev(Builder builder) {
        super(builder.fnr, builder.saksnummer, builder.dagensDato, builder.navnBruker, builder.navnMottaker,
            builder.adresselinjer, builder.postnr, builder.poststed, builder.land);
        this.datoMottatt = builder.datoMottatt;
        this.datoVedtatt = builder.datoVedtatt;
        this.datoInnsendingsfrist = builder.datoInnsendingsfrist;
        this.sakstype = builder.sakstype;
        this.behandlingstype = builder.behandlingstype;
        this.saksbehandlerNavn = builder.saksbehandlerNavn;
        this.fritekstMangelinfo = builder.fritekstMangelinfo;
        this.fritekstMottaksinfo = builder.fritekstMottaksinfo;
    }

    public Mangelbrev(DokgenBrevbestilling brevbestilling, Builder builder) throws TekniskException {
        super(brevbestilling);
        this.datoMottatt = builder.datoMottatt;
        this.datoVedtatt = builder.datoVedtatt;
        this.datoInnsendingsfrist = builder.datoInnsendingsfrist;
        this.sakstype = builder.sakstype;
        this.behandlingstype = builder.behandlingstype;
        this.saksbehandlerNavn = builder.saksbehandlerNavn;
        this.fritekstMangelinfo = builder.fritekstMangelinfo;
        this.fritekstMottaksinfo = builder.fritekstMottaksinfo;
    }

    public Instant getDatoMottatt() {
        return datoMottatt;
    }

    public Instant getDatoVedtatt() {
        return datoVedtatt;
    }

    public Instant getDatoInnsendingsfrist() {
        return datoInnsendingsfrist;
    }

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public String getSaksbehandlerNavn() {
        return saksbehandlerNavn;
    }

    public String getFritekstMangelinfo() {
        return fritekstMangelinfo;
    }

    public String getFritekstMottaksinfo() {
        return fritekstMottaksinfo;
    }

    protected static Instant hentVedtaksdato(Behandlingsresultat behandlingsresultat) {
        return (behandlingsresultat != null && behandlingsresultat.harVedtak()) ?
            behandlingsresultat.getVedtakMetadata().getVedtaksdato() : null;
    }

    public static final class Builder {
        private String fnr;
        private String saksnummer;
        private Instant dagensDato;
        private String navnBruker;
        private String navnMottaker;
        private List<String> adresselinjer;
        private String postnr;
        private String poststed;
        private String land;
        private Instant datoMottatt;
        private Instant datoVedtatt;
        private Instant datoInnsendingsfrist;
        private Sakstyper sakstype;
        private Behandlingstyper behandlingstype;
        private String saksbehandlerNavn;
        private String fritekstMangelinfo;
        private String fritekstMottaksinfo;

        public Builder medFnr(String fnr) {
            this.fnr = fnr;
            return this;
        }

        public Builder medSaksnummer(String saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder medDagensDato(Instant dagensDato) {
            this.dagensDato = dagensDato;
            return this;
        }

        public Builder medNavnBruker(String navnBruker) {
            this.navnBruker = navnBruker;
            return this;
        }

        public Builder medNavnMottaker(String navnMottaker) {
            this.navnMottaker = navnMottaker;
            return this;
        }

        public Builder medAdresselinjer(List<String> adresselinjer) {
            this.adresselinjer = adresselinjer;
            return this;
        }

        public Builder medPostnr(String postnr) {
            this.postnr = postnr;
            return this;
        }

        public Builder medPoststed(String poststed) {
            this.poststed = poststed;
            return this;
        }

        public Builder medLand(String land) {
            this.land = land;
            return this;
        }

        public Builder medDatoMottatt(Instant datoMottatt) {
            this.datoMottatt = datoMottatt;
            return this;
        }

        public Builder medDatoVedtatt(Instant datoVedtatt) {
            this.datoVedtatt = datoVedtatt;
            return this;
        }

        public Builder medDatoInnsendingsfrist(Instant datoInnsendingsfrist) {
            this.datoInnsendingsfrist = datoInnsendingsfrist;
            return this;
        }

        public Builder medSakstype(Sakstyper sakstype) {
            this.sakstype = sakstype;
            return this;
        }

        public Builder medBehandlingstype(Behandlingstyper behandlingstype) {
            this.behandlingstype = behandlingstype;
            return this;
        }

        public Builder medSaksbehandlerNavn(String saksbehandlerNavn) {
            this.saksbehandlerNavn = saksbehandlerNavn;
            return this;
        }

        public Builder medFritekstMangelinfo(String fritekstMangelinfo) {
            this.fritekstMangelinfo = fritekstMangelinfo;
            return this;
        }

        public Builder medFritekstMottaksinfo(String fritekstMottaksinfo) {
            this.fritekstMottaksinfo = fritekstMottaksinfo;
            return this;
        }

        public Mangelbrev build() {
            return new Mangelbrev(this);
        }
    }
}
