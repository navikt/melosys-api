package no.nav.melosys.domain.brev

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Kontaktopplysning
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.arkiv.Distribusjonstype
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.arkiv.SaksvedleggBestilling
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.person.Persondata
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = DokgenBrevbestilling::class)
@JsonSubTypes(
    JsonSubTypes.Type(value = MangelbrevBrevbestilling::class),
    JsonSubTypes.Type(value = InnvilgelseBrevbestilling::class),
    JsonSubTypes.Type(value = FritekstbrevBrevbestilling::class),
    JsonSubTypes.Type(value = AvslagBrevbestilling::class),
    JsonSubTypes.Type(value = HenleggelseBrevbestilling::class),
    JsonSubTypes.Type(value = FritekstvedleggBrevbestilling::class),
    JsonSubTypes.Type(value = IkkeYrkesaktivBrevbestilling::class),
    JsonSubTypes.Type(value = InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling::class),
    JsonSubTypes.Type(value = VarselbrevManglendeInnbetalingBrevbestilling::class),
    JsonSubTypes.Type(value = VedtakOpphoertMedlemskapBrevbestilling::class),
    JsonSubTypes.Type(value = InnhentingAvInntektsopplysningerBrevbestilling::class),
    JsonSubTypes.Type(value = OrienteringAnmodningUnntakBrevbestilling::class),
    JsonSubTypes.Type(value = InnvilgelseEftaStorbritanniaBrevbestilling::class),
    JsonSubTypes.Type(value = AvslagEftaStorbritanniaBrevbestilling::class),
    JsonSubTypes.Type(value = ÅrsavregningVedtakBrevBestilling::class),
    JsonSubTypes.Type(value = OrienteringTilArbeidsgiverOmVedtakBrevbestilling::class)
)
open class DokgenBrevbestilling : Brevbestilling {
    var org: OrganisasjonDokument? = null
    var kontaktopplysning: Kontaktopplysning? = null
    var utenlandskMyndighet: UtenlandskMyndighet? = null
    var kontaktpersonNavn: String? = null
    var forsendelseMottatt: Instant? = null
    var avsenderLand: String? = null
    var avsendertype: Avsendertyper? = null
    var behandlingId: Long = 0L

    @JsonAlias("isBestillKopi")
    var bestillKopi: Boolean = false

    @JsonAlias("isBestillUtkast")
    var bestillUtkast: Boolean = false

    var vedtaksdato: Instant? = null
    var saksbehandlerNavn: String? = null
    var persondokument: Persondata? = null
    var personMottaker: Persondata? = null
    var saksvedleggBestilling: List<SaksvedleggBestilling>? = null
    var standardvedleggType: StandardvedleggType? = null
    var distribusjonstype: Distribusjonstype? = null
    var fritekstvedleggBestilling: List<FritekstvedleggBestilling>? = null

    fun forsendelseMottattNonNull(): Instant = forsendelseMottatt ?: throw IllegalStateException("forsendelsemottatt kan ikke være null")

    // Empty constructor for deserialization in process instance
    constructor() : super()

    protected constructor(builder: Builder<*>) : super(
        builder.produserbartdokument,
        builder.behandling,
        builder.avsenderNavn
    ) {
        this.org = builder.org
        this.kontaktopplysning = builder.kontaktopplysning
        this.utenlandskMyndighet = builder.utenlandskMyndighet
        this.kontaktpersonNavn = builder.kontaktpersonNavn
        this.forsendelseMottatt = builder.forsendelseMottatt
        this.avsendertype = builder.avsendertype
        this.avsenderLand = builder.avsenderLand
        this.behandlingId = builder.behandlingId
        this.bestillKopi = builder.bestillKopi
        this.bestillUtkast = builder.bestillUtkast
        this.vedtaksdato = builder.vedtaksdato
        this.saksbehandlerNavn = builder.saksbehandlerNavn
        this.persondokument = builder.persondokument
        this.personMottaker = builder.personMottaker
        this.saksvedleggBestilling = builder.saksvedleggBestilling
        this.standardvedleggType = builder.standardvedleggType
        this.distribusjonstype = builder.distribusjonstype
        this.fritekstvedleggBestilling = builder.fritekstvedleggBestilling
    }

    // Java compatibility methods for boolean properties
    fun isBestillKopi(): Boolean = bestillKopi
    fun isBestillUtkast(): Boolean = bestillUtkast

    // TODO: Skriv om til interface metode. Dersom metoden ikke er overridet i subklassen vil man få en ClassCastException i DokgenMalMapper.lagDokgenDtoFraBestilling
    open fun toBuilder(): Builder<*> = Builder(this)

    open class Builder<T : Builder<T>> {
        internal var produserbartdokument: Produserbaredokumenter? = null
        internal var behandling: Behandling? = null
        internal var org: OrganisasjonDokument? = null
        internal var kontaktopplysning: Kontaktopplysning? = null
        internal var utenlandskMyndighet: UtenlandskMyndighet? = null
        internal var kontaktpersonNavn: String? = null
        internal var forsendelseMottatt: Instant? = null
        internal var avsenderNavn: String? = null
        internal var avsenderLand: String? = null
        internal var avsendertype: Avsendertyper? = null
        internal var behandlingId: Long = 0L
        internal var bestillKopi: Boolean = false
        internal var bestillUtkast: Boolean = false
        internal var vedtaksdato: Instant? = null
        internal var saksbehandlerNavn: String? = null
        internal var persondokument: Persondata? = null
        internal var personMottaker: Persondata? = null
        internal var saksvedleggBestilling: List<SaksvedleggBestilling>? = null
        internal var standardvedleggType: StandardvedleggType? = null
        internal var distribusjonstype: Distribusjonstype? = null
        internal var fritekstvedleggBestilling: List<FritekstvedleggBestilling>? = null

        constructor()

        constructor(brevbestilling: DokgenBrevbestilling) {
            this.produserbartdokument = brevbestilling.produserbartdokument
            this.behandling = brevbestilling.behandling
            this.org = brevbestilling.org
            this.kontaktopplysning = brevbestilling.kontaktopplysning
            this.utenlandskMyndighet = brevbestilling.utenlandskMyndighet
            this.kontaktpersonNavn = brevbestilling.kontaktpersonNavn
            this.forsendelseMottatt = brevbestilling.forsendelseMottatt
            this.avsenderNavn = brevbestilling.avsenderID
            this.avsendertype = brevbestilling.avsendertype
            this.avsenderLand = brevbestilling.avsenderLand
            this.behandlingId = brevbestilling.behandlingId
            this.bestillKopi = brevbestilling.bestillKopi
            this.bestillUtkast = brevbestilling.bestillUtkast
            this.vedtaksdato = brevbestilling.vedtaksdato
            this.saksbehandlerNavn = brevbestilling.saksbehandlerNavn
            this.persondokument = brevbestilling.persondokument
            this.personMottaker = brevbestilling.personMottaker
            this.saksvedleggBestilling = brevbestilling.saksvedleggBestilling
            this.standardvedleggType = brevbestilling.standardvedleggType
            this.distribusjonstype = brevbestilling.distribusjonstype
            this.fritekstvedleggBestilling = brevbestilling.fritekstvedleggBestilling
        }

        @Suppress("UNCHECKED_CAST")
        fun medProduserbartdokument(produserbartdokument: Produserbaredokumenter?): T =
            apply { this.produserbartdokument = produserbartdokument } as T

        @Suppress("UNCHECKED_CAST")
        fun medBehandling(behandling: Behandling?): T = apply { this.behandling = behandling } as T

        @Suppress("UNCHECKED_CAST")
        fun medOrg(org: OrganisasjonDokument?): T = apply { this.org = org } as T

        @Suppress("UNCHECKED_CAST")
        fun medKontaktopplysning(kontaktopplysning: Kontaktopplysning?): T = apply { this.kontaktopplysning = kontaktopplysning } as T

        @Suppress("UNCHECKED_CAST")
        fun medUtenlandskMyndighet(utenlandskMyndighet: UtenlandskMyndighet?): T = apply { this.utenlandskMyndighet = utenlandskMyndighet } as T

        @Suppress("UNCHECKED_CAST")
        fun medKontaktpersonNavn(kontaktpersonNavn: String?): T = apply { this.kontaktpersonNavn = kontaktpersonNavn } as T

        @Suppress("UNCHECKED_CAST")
        fun medForsendelseMottatt(forsendelseMottatt: Instant?): T = apply { this.forsendelseMottatt = forsendelseMottatt } as T

        @Suppress("UNCHECKED_CAST")
        fun medAvsenderNavn(avsenderNavn: String?): T = apply { this.avsenderNavn = avsenderNavn } as T

        @Suppress("UNCHECKED_CAST")
        fun medAvsendertype(avsendertype: Avsendertyper?): T = apply { this.avsendertype = avsendertype } as T

        @Suppress("UNCHECKED_CAST")
        fun medAvsenderLand(avsenderLand: String?): T = apply { this.avsenderLand = avsenderLand } as T

        @Suppress("UNCHECKED_CAST")
        fun medAvsenderFraJournalpost(journalpost: Journalpost): T = apply {
            this.avsenderNavn = journalpost.avsenderNavn
            this.avsendertype = journalpost.avsenderType
            this.avsenderLand = journalpost.avsenderLand
        } as T

        @Suppress("UNCHECKED_CAST")
        fun medBehandlingId(behandlingId: Long): T = apply { this.behandlingId = behandlingId } as T

        @Suppress("UNCHECKED_CAST")
        fun medBestillKopi(bestillKopi: Boolean): T = apply { this.bestillKopi = bestillKopi } as T

        @Suppress("UNCHECKED_CAST")
        fun medBestillUtkast(bestillUtkast: Boolean): T = apply { this.bestillUtkast = bestillUtkast } as T

        @Suppress("UNCHECKED_CAST")
        fun medVedtaksdato(vedtaksdato: Instant?): T = apply { this.vedtaksdato = vedtaksdato } as T

        @Suppress("UNCHECKED_CAST")
        fun medSaksbehandlerNavn(saksbehandlerNavn: String?): T = apply { this.saksbehandlerNavn = saksbehandlerNavn } as T

        @Suppress("UNCHECKED_CAST")
        fun medPersonDokument(persondata: Persondata?): T = apply { this.persondokument = persondata } as T

        @Suppress("UNCHECKED_CAST")
        fun medPersonMottaker(personMottaker: Persondata?): T = apply { this.personMottaker = personMottaker } as T

        @Suppress("UNCHECKED_CAST")
        fun medSaksvedleggBestilling(saksvedleggBestilling: List<SaksvedleggBestilling>?): T = apply {
            this.saksvedleggBestilling = saksvedleggBestilling
        } as T

        @Suppress("UNCHECKED_CAST")
        fun medStandardvedleggBestilling(standardvedleggType: StandardvedleggType?): T = apply { this.standardvedleggType = standardvedleggType } as T

        @Suppress("UNCHECKED_CAST")
        fun medDistribusjonstype(distribusjonstype: Distribusjonstype?): T = apply { this.distribusjonstype = distribusjonstype } as T

        @Suppress("UNCHECKED_CAST")
        fun medFritekstvedleggBestilling(fritekstvedleggBestilling: List<FritekstvedleggBestilling>?): T = apply {
            this.fritekstvedleggBestilling = fritekstvedleggBestilling
        } as T

        open fun build(): DokgenBrevbestilling = DokgenBrevbestilling(this)
    }
}
