package no.nav.melosys.domain.brev;

import java.util.Arrays;
import java.util.Collection;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public final class DoksysBrevbestilling extends Brevbestilling {
    private final Aktoersroller mottakerRolle;
    private final Collection<Mottaker> mottaker; //NOTE Flytt opp til Brevbestilling
    private final String begrunnelseKode;
    private final String fritekst;
    private final String ytterligereInformasjon;

    private DoksysBrevbestilling(Produserbaredokumenter produserbartdokument,
                                 String avsenderNavn,
                                 Aktoersroller mottakerRolle,
                                 Collection<Mottaker> mottaker,
                                 Behandling behandling,
                                 String begrunnelseKode,
                                 String fritekst,
                                 String ytterligereInformasjon) {
        super(produserbartdokument, behandling, avsenderNavn);
        this.mottakerRolle = mottakerRolle;
        this.mottaker = mottaker;
        this.begrunnelseKode = begrunnelseKode;
        this.fritekst = fritekst;
        this.ytterligereInformasjon = ytterligereInformasjon;
    }

    public static class Builder {
        private Produserbaredokumenter produserbartdokument;
        private String avsenderNavn;
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

        public Builder medAvsenderNavn(String avsenderNavn) {
            this.avsenderNavn = avsenderNavn;
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
                avsenderNavn,
                mottakerRolle,
                mottakere,
                behandling,
                begrunnelseKode,
                fritekst,
                ytterligereInformasjon
            );
        }
    }

    public Aktoersroller getMottakerRolle() {
        return mottakerRolle;
    }

    public Collection<Mottaker> getMottakere() {
        return mottaker;
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
}
