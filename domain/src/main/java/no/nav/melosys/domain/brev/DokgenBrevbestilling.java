package no.nav.melosys.domain.brev;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.SaksvedleggBestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.person.Persondata;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = DokgenBrevbestilling.class)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = MangelbrevBrevbestilling.class),
        @JsonSubTypes.Type(value = InnvilgelseBrevbestilling.class),
        @JsonSubTypes.Type(value = FritekstbrevBrevbestilling.class),
        @JsonSubTypes.Type(value = AvslagBrevbestilling.class),
        @JsonSubTypes.Type(value = HenleggelseBrevbestilling.class),
        @JsonSubTypes.Type(value = FritekstvedleggBrevbestilling.class),
        @JsonSubTypes.Type(value = IkkeYrkesaktivBrevbestilling.class),
        @JsonSubTypes.Type(value = InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling.class),
        @JsonSubTypes.Type(value = VarselbrevManglendeInnbetalingBrevbestilling.class),
        @JsonSubTypes.Type(value = VedtakOpphoertMedlemskapBrevbestilling.class),
        @JsonSubTypes.Type(value = InnhentingAvInntektsopplysningerBrevbestilling.class),
        @JsonSubTypes.Type(value = OrienteringAnmodningUnntakBrevbestilling.class),
        @JsonSubTypes.Type(value = InnvilgelseEftaStorbritanniaBrevbestilling.class),
    }
)
public class DokgenBrevbestilling extends Brevbestilling {
    private OrganisasjonDokument org;
    private Kontaktopplysning kontaktopplysning;
    private UtenlandskMyndighet utenlandskMyndighet;
    private String kontaktpersonNavn;
    private Instant forsendelseMottatt;
    private String avsenderLand;
    private Avsendertyper avsendertype;
    private long behandlingId;
    private boolean bestillKopi;
    private boolean bestillUtkast;
    private Instant vedtaksdato;
    private String saksbehandlerNavn;
    private Persondata persondokument;
    private Persondata personMottaker;
    private List<SaksvedleggBestilling> saksvedleggBestilling;
    private Distribusjonstype distribusjonstype;
    private List<FritekstvedleggBestilling> fritekstvedleggBestilling;

    public DokgenBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    protected DokgenBrevbestilling(Builder<?> builder) {
        super(builder.produserbartdokument, builder.behandling, builder.avsenderNavn);
        this.org = builder.org;
        this.kontaktopplysning = builder.kontaktopplysning;
        this.utenlandskMyndighet = builder.utenlandskMyndighet;
        this.kontaktpersonNavn = builder.kontaktpersonNavn;
        this.forsendelseMottatt = builder.forsendelseMottatt;
        this.avsendertype = builder.avsendertype;
        this.avsenderLand = builder.avsenderLand;
        this.behandlingId = builder.behandlingId;
        this.bestillKopi = builder.bestillKopi;
        this.bestillUtkast = builder.bestillUtkast;
        this.vedtaksdato = builder.vedtaksdato;
        this.saksbehandlerNavn = builder.saksbehandlerNavn;
        this.persondokument = builder.persondokument;
        this.personMottaker = builder.personMottaker;
        this.saksvedleggBestilling = builder.saksvedleggBestilling;
        this.distribusjonstype = builder.distribusjonstype;
        this.fritekstvedleggBestilling = builder.fritekstvedleggBestilling;
    }

    public OrganisasjonDokument getOrg() {
        return org;
    }

    public Kontaktopplysning getKontaktopplysning() {
        return kontaktopplysning;
    }

    public UtenlandskMyndighet getUtenlandskMyndighet() {
        return utenlandskMyndighet;
    }

    public String getKontaktpersonNavn() {
        return kontaktpersonNavn;
    }

    public Instant getForsendelseMottatt() {
        return forsendelseMottatt;
    }

    public String getAvsenderLand() {
        return avsenderLand;
    }

    public Avsendertyper getAvsendertype() {
        return avsendertype;
    }

    public long getBehandlingId() {
        return behandlingId;
    }

    public boolean isBestillKopi() {
        return bestillKopi;
    }

    public boolean isBestillUtkast() {
        return bestillUtkast;
    }

    public Instant getVedtaksdato() {
        return vedtaksdato;
    }

    public String getSaksbehandlerNavn() {
        return saksbehandlerNavn;
    }

    public Persondata getPersondokument() {
        return persondokument;
    }

    public Persondata getPersonMottaker() {
        return personMottaker;
    }

    public List<SaksvedleggBestilling> getSaksvedleggBestilling() {
        return saksvedleggBestilling;
    }

    public List<FritekstvedleggBestilling> getFritekstvedleggBestilling() {
        return fritekstvedleggBestilling;
    }

    public Distribusjonstype getDistribusjonstype() {
        return distribusjonstype;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder<T extends Builder<T>> {
        private Produserbaredokumenter produserbartdokument;
        private Behandling behandling;
        private OrganisasjonDokument org;
        private Kontaktopplysning kontaktopplysning;
        private UtenlandskMyndighet utenlandskMyndighet;
        private String kontaktpersonNavn;
        private Instant forsendelseMottatt;
        private String avsenderNavn;
        private String avsenderLand;
        private Avsendertyper avsendertype;
        private long behandlingId;
        private boolean bestillKopi;
        private boolean bestillUtkast;
        private Instant vedtaksdato;
        private String saksbehandlerNavn;
        private Persondata persondokument;
        private Persondata personMottaker;
        private List<SaksvedleggBestilling> saksvedleggBestilling;
        private Distribusjonstype distribusjonstype;
        private List<FritekstvedleggBestilling> fritekstvedleggBestilling;


        public Builder() {
        }

        public Builder(DokgenBrevbestilling brevbestilling) {
            this.produserbartdokument = brevbestilling.produserbartdokument;
            this.behandling = brevbestilling.behandling;
            this.org = brevbestilling.org;
            this.kontaktopplysning = brevbestilling.kontaktopplysning;
            this.utenlandskMyndighet = brevbestilling.utenlandskMyndighet;
            this.kontaktpersonNavn = brevbestilling.kontaktpersonNavn;
            this.forsendelseMottatt = brevbestilling.forsendelseMottatt;
            this.avsenderNavn = brevbestilling.avsenderID;
            this.avsendertype = brevbestilling.avsendertype;
            this.avsenderLand = brevbestilling.avsenderLand;
            this.behandlingId = brevbestilling.behandlingId;
            this.bestillKopi = brevbestilling.bestillKopi;
            this.bestillUtkast = brevbestilling.bestillUtkast;
            this.vedtaksdato = brevbestilling.vedtaksdato;
            this.saksbehandlerNavn = brevbestilling.saksbehandlerNavn;
            this.persondokument = brevbestilling.persondokument;
            this.personMottaker = brevbestilling.personMottaker;
            this.saksvedleggBestilling = brevbestilling.saksvedleggBestilling;
            this.distribusjonstype = brevbestilling.distribusjonstype;
            this.fritekstvedleggBestilling = brevbestilling.fritekstvedleggBestilling;
        }

        public T medProduserbartdokument(Produserbaredokumenter produserbartdokument) {
            this.produserbartdokument = produserbartdokument;
            return (T) this;
        }

        public T medBehandling(Behandling behandling) {
            this.behandling = behandling;
            return (T) this;
        }

        public T medOrg(OrganisasjonDokument org) {
            this.org = org;
            return (T) this;
        }

        public T medKontaktopplysning(Kontaktopplysning kontaktopplysning) {
            this.kontaktopplysning = kontaktopplysning;
            return (T) this;
        }

        public T medUtenlandskMyndighet(UtenlandskMyndighet utenlandskMyndighet) {
            this.utenlandskMyndighet = utenlandskMyndighet;
            return (T) this;
        }

        public T medKontaktpersonNavn(String kontaktpersonNavn) {
            this.kontaktpersonNavn = kontaktpersonNavn;
            return (T) this;
        }

        public T medForsendelseMottatt(Instant forsendelseMottatt) {
            this.forsendelseMottatt = forsendelseMottatt;
            return (T) this;
        }

        public T medAvsenderNavn(String avsenderNavn) {
            this.avsenderNavn = avsenderNavn;
            return (T) this;
        }

        public T medAvsendertype(Avsendertyper avsendertype) {
            this.avsendertype = avsendertype;
            return (T) this;
        }

        public T medAvsenderLand(String avsenderLand) {
            this.avsenderLand = avsenderLand;
            return (T) this;
        }

        public T medAvsenderFraJournalpost(Journalpost journalpost) {
            this.avsenderNavn = journalpost.getAvsenderNavn();
            this.avsendertype = journalpost.getAvsenderType();
            this.avsenderLand = journalpost.getAvsenderLand();
            return (T) this;
        }

        public T medBehandlingId(long behandlingId) {
            this.behandlingId = behandlingId;
            return (T) this;
        }

        public T medBestillKopi(boolean bestillKopi) {
            this.bestillKopi = bestillKopi;
            return (T) this;
        }

        public T medBestillUtkast(boolean bestillUtkast) {
            this.bestillUtkast = bestillUtkast;
            return (T) this;
        }

        public T medVedtaksdato(Instant vedtaksdato) {
            this.vedtaksdato = vedtaksdato;
            return (T) this;
        }

        public T medSaksbehandlerNavn(String saksbehandlerNavn) {
            this.saksbehandlerNavn = saksbehandlerNavn;
            return (T) this;
        }

        public T medPersonDokument(Persondata persondata) {
            this.persondokument = persondata;
            return (T) this;
        }

        public T medPersonMottaker(Persondata personMottaker) {
            this.personMottaker = personMottaker;
            return (T) this;
        }

        public T medSaksvedleggBestilling(List<SaksvedleggBestilling> saksvedleggBestilling) {
            this.saksvedleggBestilling = saksvedleggBestilling;
            return (T) this;
        }

        public T medDistribusjonstype(Distribusjonstype distribusjonstype) {
            this.distribusjonstype = distribusjonstype;
            return (T) this;
        }

        public T medFritekstvedleggBestilling(List<FritekstvedleggBestilling> fritekstvedleggBestilling) {
            this.fritekstvedleggBestilling = fritekstvedleggBestilling;
            return (T) this;
        }

        public DokgenBrevbestilling build() {
            return new DokgenBrevbestilling(this);
        }
    }
}
