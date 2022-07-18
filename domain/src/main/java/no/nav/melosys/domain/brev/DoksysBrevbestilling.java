package no.nav.melosys.domain.brev;

import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = DoksysBrevbestilling.class)
public class DoksysBrevbestilling extends Brevbestilling {
    private Aktoersroller mottakerRolle;
    private Collection<Mottaker> mottakere; //NOTE Flytt opp til Brevbestilling
    private String begrunnelseKode;
    private String fritekst;
    private String ytterligereInformasjon;

    public DoksysBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    protected DoksysBrevbestilling(Produserbaredokumenter produserbartdokument,
                                   String avsenderID,
                                   Aktoersroller mottakerRolle,
                                   Collection<Mottaker> mottakere,
                                   Behandling behandling,
                                   String begrunnelseKode,
                                   String fritekst,
                                   String ytterligereInformasjon) {
        super(produserbartdokument, behandling, avsenderID);
        this.mottakerRolle = mottakerRolle;
        this.mottakere = mottakere;
        this.begrunnelseKode = begrunnelseKode;
        this.fritekst = fritekst;
        this.ytterligereInformasjon = ytterligereInformasjon;
    }

    public Aktoersroller getMottakerRolle() {
        return mottakerRolle;
    }

    public Collection<Mottaker> getMottakere() {
        return mottakere;
    }

    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public String getFritekst() {
        return fritekst;
    }

    public String getYtterligereInformasjon() {
        return ytterligereInformasjon;
    }

    public static class Builder {
        private Produserbaredokumenter produserbartdokument;
        private String avsenderID;
        private Aktoersroller mottakerRolle;
        private Collection<Mottaker> mottakere;
        private Behandling behandling;
        private String begrunnelseKode;
        private String fritekst;
        private String ytterligereInformasjon;

        public Builder medProduserbartDokument(Produserbaredokumenter produserbartdokument) {
            this.produserbartdokument = produserbartdokument;
            return this;
        }

        public Builder medAvsenderID(String avsenderID) {
            this.avsenderID = avsenderID;
            return this;
        }

        public Builder medMottakerRolle(Aktoersroller mottakerRolle) {
            this.mottakerRolle = mottakerRolle;
            return this;
        }


        public Builder medMottakere(Mottaker... mottakere) {
            this.mottakere = Arrays.asList(mottakere);
            return this;
        }

        public Builder medMottakere(Collection<Mottaker> mottakere) {
            this.mottakere = mottakere;
            return this;
        }

        public Builder medBehandling(Behandling behandling) {
            this.behandling = behandling;
            return this;
        }

        public Builder medBegrunnelseKode(String begrunnelseKode) {
            this.begrunnelseKode = begrunnelseKode;
            return this;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public Builder medYtterligereInformasjon(String ytterligereInformasjon) {
            this.ytterligereInformasjon = ytterligereInformasjon;
            return this;
        }

        public DoksysBrevbestilling build() {
            return new DoksysBrevbestilling(
                produserbartdokument,
                avsenderID,
                mottakerRolle,
                mottakere,
                behandling,
                begrunnelseKode,
                fritekst,
                ytterligereInformasjon
            );
        }
    }

}
