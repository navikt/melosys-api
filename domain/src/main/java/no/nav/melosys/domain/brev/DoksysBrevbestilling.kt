package no.nav.melosys.domain.brev

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.arkiv.Distribusjonstype
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter

class DoksysBrevbestilling : Brevbestilling {
    val mottakerRolle: Mottakerroller?
    var mottakere: Collection<Mottaker>?
    val begrunnelseKode: String?
    val fritekst: String?
    val distribusjonstype: Distribusjonstype?
    val ytterligereInformasjon: String?

    private constructor(
        produserbartdokument: Produserbaredokumenter?,
        avsenderID: String?,
        mottakerRolle: Mottakerroller?,
        mottakere: Collection<Mottaker>?,
        behandling: Behandling?,
        begrunnelseKode: String?,
        fritekst: String?,
        ytterligereInformasjon: String?,
        distribusjonstype: Distribusjonstype?
    ) : super(produserbartdokument, behandling, avsenderID) {
        this.mottakerRolle = mottakerRolle
        this.mottakere = mottakere
        this.begrunnelseKode = begrunnelseKode
        this.fritekst = fritekst
        this.ytterligereInformasjon = ytterligereInformasjon
        this.distribusjonstype = distribusjonstype
    }

    fun settMottaker(mottaker: Mottaker?) {
        mottakere = mottaker?.let { listOf(it) }
    }

    class Builder {
        private var produserbartdokument: Produserbaredokumenter? = null
        private var avsenderID: String? = null
        private var mottakerRolle: Mottakerroller? = null
        private var mottakere: Collection<Mottaker>? = null
        private var behandling: Behandling? = null
        private var begrunnelseKode: String? = null
        private var fritekst: String? = null
        private var distribusjonstype: Distribusjonstype? = null
        private var ytterligereInformasjon: String? = null

        fun medProduserbartDokument(produserbartdokument: Produserbaredokumenter?) = apply { this.produserbartdokument = produserbartdokument }

        fun medAvsenderID(avsenderID: String?) = apply { this.avsenderID = avsenderID }

        fun medMottakerRolle(mottakerRolle: Mottakerroller?) = apply { this.mottakerRolle = mottakerRolle }

        fun medMottakere(vararg mottakere: Mottaker) = apply { this.mottakere = mottakere.toList() }

        fun medMottakere(mottakere: Collection<Mottaker>?) = apply { this.mottakere = mottakere }

        fun medBehandling(behandling: Behandling?) = apply { this.behandling = behandling }

        fun medBegrunnelseKode(begrunnelseKode: String?) = apply { this.begrunnelseKode = begrunnelseKode }

        fun medFritekst(fritekst: String?) = apply { this.fritekst = fritekst }

        fun medDistribusjonsType(distribusjonstype: Distribusjonstype?) = apply { this.distribusjonstype = distribusjonstype }

        fun medYtterligereInformasjon(ytterligereInformasjon: String?) = apply { this.ytterligereInformasjon = ytterligereInformasjon }

        fun build() = DoksysBrevbestilling(
            produserbartdokument = produserbartdokument,
            avsenderID = avsenderID,
            mottakerRolle = mottakerRolle,
            mottakere = mottakere,
            behandling = behandling,
            begrunnelseKode = begrunnelseKode,
            fritekst = fritekst,
            ytterligereInformasjon = ytterligereInformasjon,
            distribusjonstype = distribusjonstype
        )
    }
}
