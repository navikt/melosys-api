package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

/**
 * Kastes når en skarp kjøring avvises av en sikkerhetssele. Controlleren gjør dette om til 400.
 */
class VedtaksmetadataFiksAvvist(melding: String) : RuntimeException(melding)

/**
 * Datafiks for MELOSYS-8174: setter inn manglende rader i `vedtak_metadata` for avsluttede
 * behandlinger som blokkerer skattepliktig årsavregning.
 *
 * `ÅrsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning` kaller
 * `hentVedtakMetadata()` ubetinget i filter/sortering, så én rad uten vedtaksmetadata velter hele
 * saken med «vedtakMetadata er påkrevd for Behandlingsresultat» før den blir faglig vurdert.
 *
 * Fiksen kjøres med native SQL, ikke via JPA, av to grunner:
 *  - `registrert_av`/`endret_av` må bli [PATCH_MARKØR] slik at fiksen kan rulles tilbake ([angre]).
 *    JPA-auditing ville satt saksbehandler/«MELOSYS» i stedet.
 *  - `vedtak_dato` skal være proxyen `behandlingsresultat.endret_dato`, ikke `Instant.now()` som
 *    `Behandlingsresultat.settVedtakMetadata` bruker.
 *
 * Innsettingen er idempotent (`NOT EXISTS`), så en utilsiktet ny kjøring gir null nye rader.
 */
@Component
class VedtaksmetadataFiksService {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /** Read-only preview av nøyaktig hvilke rader [utfør] vil sette inn. */
    @Transactional(readOnly = true)
    fun forhåndsvis(
        saksnummer: List<String>,
        tillatSorteringsendring: List<String> = emptyList(),
    ): VedtaksmetadataFiksResultat {
        val kandidater = hentKandidater(saksnummer)
        val påvirkning = sorteringspåvirkning(saksnummer)
        return VedtaksmetadataFiksResultat(
            skarp = false,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallRaderInnsatt = 0,
            rader = kandidater,
            utenMetadataPerSak = tellUtenMetadata(saksnummer),
            ukjentBehType = kandidater.filterNot { it.behType in KJENTE_BEH_TYPER }.map { it.behandlingsresultatId },
            sorteringspåvirkning = påvirkning,
            saksnummerUtenKandidater = finnSaksnummerUtenKandidater(saksnummer, kandidater),
            kvitteringerUtenTreff = finnKvitteringerUtenTreff(tillatSorteringsendring, påvirkning),
        )
    }

    /**
     * Datafiksen — endrer prod. Avvises hvis scope er tomt, kandidatantallet overstiger
     * [maksAntallRader], en kandidat har ukjent `beh_type`, eller patchen tar nyeste-plassen i
     * vedtaksdato-sorteringen (se [sorteringspåvirkning]) for en sak som ikke er kvittert ut.
     *
     * [tillatSorteringsendring] er en liste med saksnummer, ikke et av/på-flagg: den vurderte saken
     * kvitteres ut uten at selen slås av for de øvrige sakene i samme kall.
     */
    @Transactional
    fun utfør(
        saksnummer: List<String>,
        maksAntallRader: Int,
        tillatSorteringsendring: List<String> = emptyList(),
    ): VedtaksmetadataFiksResultat {
        if (saksnummer.isEmpty()) throw VedtaksmetadataFiksAvvist("Skarp kjøring krever eksplisitt saksnummer-liste")

        val kandidater = hentKandidater(saksnummer)
        if (kandidater.size > maksAntallRader) {
            throw VedtaksmetadataFiksAvvist(
                "Fant ${kandidater.size} rader, men maksAntallRader er $maksAntallRader. " +
                    "Kjør preview først, og hev maksAntallRader eksplisitt hvis tallet er riktig."
            )
        }
        // Etter maksAntallRader-selen, så den mer presise meldingen vinner i det vanlige tilfellet.
        if (kandidater.size > MAKS_UTTRYKK_I_IN) {
            throw VedtaksmetadataFiksAvvist(
                "Fant ${kandidater.size} kandidater, men INSERT-en binder dem i en IN-liste, og Oracle " +
                    "tar maks $MAKS_UTTRYKK_I_IN uttrykk (ORA-01795). Del kjøringen opp i flere kall med " +
                    "færre saksnummer om gangen."
            )
        }
        val ukjente = kandidater.filterNot { it.behType in KJENTE_BEH_TYPER }
        if (ukjente.isNotEmpty()) {
            throw VedtaksmetadataFiksAvvist(
                "Kandidater med ukjent beh_type (vedtakstype kan ikke utledes trygt): " +
                    ukjente.joinToString { "${it.behandlingsresultatId}=${it.behType}" }
            )
        }

        // Må måles før INSERT-en — etterpå er patch-radene ekte rader, og «før»-bildet kan ikke gjenskapes.
        val påvirkning = sorteringspåvirkning(saksnummer)
        val kaprer = påvirkning.filter { it.patchenVinnerNyeste }
        val ikkeKvittert = kaprer.filterNot { it.saksnummer in tillatSorteringsendring }
        val kvitteringerUtenTreff = finnKvitteringerUtenTreff(tillatSorteringsendring, påvirkning)
        if (ikkeKvittert.isNotEmpty()) {
            throw VedtaksmetadataFiksAvvist(
                "Patchen ville tatt nyeste-plassen i vedtaksdato-sorteringen for " +
                    ikkeKvittert.joinToString { "${it.saksnummer} (${it.nyesteKandidatDato} mot ${it.nyesteSammenlignbareDato})" } +
                    ". Da bytter ÅrsavregningService hvilken behandling avgiftsgrunnlaget hentes fra. " +
                    "Sett ekte vedtaksdato manuelt, eller list saksnummeret i tillatSorteringsendring " +
                    "hvis endringen er vurdert og ønsket." +
                    if (kvitteringerUtenTreff.isEmpty()) "" else
                        " Merk at disse kvitteringene ikke traff noen sak som kaprer: $kvitteringerUtenTreff."
            )
        }

        kaprer.forEach {
            log.warn {
                "Datafiks $PATCH_MARKØR: sak ${it.saksnummer} patches selv om behandlingsresultat " +
                    "${it.nyesteKandidatId} (${it.nyesteKandidatDato}) tar nyeste-plassen fra " +
                    "${it.nyesteSammenlignbareId} (${it.nyesteSammenlignbareDato}) — kvittert ut i " +
                    "tillatSorteringsendring."
            }
        }

        // Patchen kan bli nyeste rad uten at selen slo ut — typisk fordi den kun fortrenger en
        // tidligere patch. Logges på INFO slik at kjøringen kan rekonstrueres i ettertid.
        påvirkning.filter { it.patchenBlirNyesteIHeleSaken && !it.patchenVinnerNyeste }.forEach {
            log.info {
                "Datafiks $PATCH_MARKØR: sak ${it.saksnummer} — behandlingsresultat ${it.nyesteKandidatId} " +
                    "(${it.nyesteKandidatDato}) blir nyeste rad i saken, men fortrenger ingen dato som " +
                    "kan være ekte (nyeste sammenlignbare: ${it.nyesteSammenlignbareDato ?: "ingen"})."
            }
        }

        log.info {
            "Datafiks $PATCH_MARKØR: setter inn ${kandidater.size} rader i vedtak_metadata for saker $saksnummer " +
                "(behandlingsresultat ${kandidater.map { it.behandlingsresultatId }})"
        }

        // INSERT-en bindes til ID-ene selene er evaluert på, ikke til kandidatfilteret på nytt:
        // Oracle er READ COMMITTED, så en re-evaluering kunne skrevet en kandidat som dukket opp
        // etter selene. Bindingen gjør også `antallInnsatt != kandidater.size` til en eksakt avvikssjekk.
        val antallInnsatt = entityManager.createNativeQuery(INSERT_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .setParameter("ider", kandidater.map { it.behandlingsresultatId })
            .setParameter("markoer", PATCH_MARKØR)
            .executeUpdate()

        val avvik = antallInnsatt != kandidater.size
        if (avvik) {
            log.warn {
                "Datafiks $PATCH_MARKØR: satte inn $antallInnsatt rader, men forhåndsvisningen viste " +
                    "${kandidater.size}. Noe har endret dataene under kjøringen — kontroller før du går videre."
            }
        } else {
            log.info { "Datafiks $PATCH_MARKØR: satte inn $antallInnsatt rader" }
        }

        return VedtaksmetadataFiksResultat(
            skarp = true,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallRaderInnsatt = antallInnsatt,
            rader = kandidater,
            utenMetadataPerSak = tellUtenMetadata(saksnummer),
            avvik = avvik,
            sorteringspåvirkning = påvirkning,
            saksnummerUtenKandidater = finnSaksnummerUtenKandidater(saksnummer, kandidater),
            kvitteringerUtenTreff = kvitteringerUtenTreff,
        )
    }

    /**
     * Angreknappen. Sletter kun rader der BÅDE `registrert_av` og `endret_av` er [PATCH_MARKØR]:
     * `registrert_av` (`@CreatedBy`) settes kun ved insert, så en rad som senere har fått en ekte
     * vedtaksdato skrevet av en saksbehandler røres ikke — den telles i `antallSomIkkeKanAngres`.
     *
     * Tom `saksnummer`-liste betyr «alle markerte rader»; `skarp = false` viser hva som ville blitt
     * slettet. Skarp kjøring uten scope krever [bekreftAlle]: et glemt `saksnummer` ville ellers
     * rullet tilbake alle patch-rader i basen — også fra tidligere kjøringer — og hver slettet rad
     * gjeninnfører 8174-krasjen på saken den fikset.
     */
    @Transactional
    fun angre(saksnummer: List<String>, skarp: Boolean, bekreftAlle: Boolean = false): VedtaksmetadataAngreResultat {
        if (skarp && saksnummer.isEmpty() && !bekreftAlle) {
            throw VedtaksmetadataFiksAvvist(
                "Skarp angre uten saksnummer sletter alle rader merket $PATCH_MARKØR, også fra tidligere " +
                    "kjøringer. Angi saksnummer, eller send bekreftAlle=true hvis du faktisk vil rulle tilbake alt."
            )
        }

        val kandidater = hentAngreKandidater(saksnummer)
        val kanIkkeAngres = tellSomIkkeKanAngres(saksnummer)

        if (!skarp) {
            return VedtaksmetadataAngreResultat(
                skarp = false,
                saksnummer = saksnummer,
                antallRaderFunnet = kandidater.size,
                antallSlettet = 0,
                rader = kandidater,
                antallSomIkkeKanAngres = kanIkkeAngres,
            )
        }

        log.info {
            "Datafiks $PATCH_MARKØR: ruller tilbake ${kandidater.size} rader " +
                "(scope=${saksnummer.ifEmpty { listOf("ALLE") }}, urørt fordi de er endret etterpå: $kanIkkeAngres)"
        }

        val sql = if (saksnummer.isEmpty()) ANGRE_SQL else ANGRE_SQL + ANGRE_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKØR)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)
        val antallSlettet = query.executeUpdate()

        log.info { "Datafiks $PATCH_MARKØR: slettet $antallSlettet rader" }

        return VedtaksmetadataAngreResultat(
            skarp = true,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallSlettet = antallSlettet,
            rader = kandidater,
            antallSomIkkeKanAngres = kanIkkeAngres,
            avvik = antallSlettet != kandidater.size,
        )
    }

    /** Saksnummer i requesten som ikke ga én eneste kandidatrad — typisk en skrivefeil, ellers usynlig i svaret. */
    private fun finnSaksnummerUtenKandidater(saksnummer: List<String>, kandidater: List<VedtaksmetadataFiksRad>): List<String> {
        val truffet = kandidater.map { it.saksnummer }.toSet()
        return saksnummer.filterNot { it in truffet }
    }

    /**
     * Kvitteringer i `tillatSorteringsendring` som ikke traff noen sak der selen slår ut — typisk en
     * skrivefeil. Beregnes også i forhåndsvisningen, så feilen oppdages før kjøringen som endrer prod.
     */
    private fun finnKvitteringerUtenTreff(
        tillatSorteringsendring: List<String>,
        påvirkning: List<SorteringspåvirkningRad>,
    ): List<String> {
        val kaprer = påvirkning.filter { it.patchenVinnerNyeste }.map { it.saksnummer }.toSet()
        return tillatSorteringsendring.distinct().filterNot { it in kaprer }
    }

    @Suppress("UNCHECKED_CAST")
    private fun hentKandidater(saksnummer: List<String>): List<VedtaksmetadataFiksRad> {
        if (saksnummer.isEmpty()) return emptyList()

        val rader = entityManager.createNativeQuery(KANDIDAT_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .resultList as List<Array<Any?>>

        return rader.map { rad ->
            VedtaksmetadataFiksRad(
                saksnummer = rad[0] as String,
                behandlingsresultatId = (rad[1] as Number).toLong(),
                behType = rad[2] as String?,
                resultatType = rad[3] as String?,
                blirVedtakDato = rad[4] as String?,
                blirKlagefrist = rad[5] as String?,
                blirVedtakType = rad[6] as String,
            )
        }
    }

    /**
     * Viser hva patchen gjør med vedtaksdato-sorteringen, per sak.
     *
     * `vedtak_dato` er ikke dekorasjon: `ÅrsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning`
     * sorterer på den og plukker den siste som `sisteBehandlingsresultatMedAvgift` — altså behandlingen
     * avgiftsgrunnlaget hentes fra. Proxyen `endret_dato` er `@LastModifiedDate` og alltid ≥ ekte
     * vedtaksdato, så patchede rader ser systematisk nyere ut enn de er.
     *
     * Selen skal hindre at en oppdiktet dato fortrenger et vedtak som kan være ekte, og sammenligner
     * derfor kun mot [Datoopphav.EKTE] og [Datoopphav.PATCHET_ENDRET]. Rader vi selv satte inn og som
     * ingen har rørt siden er beviselig vår egen proxy; de rapporteres, men styrer ingenting.
     * [SorteringspåvirkningRad.patchenVinnerNyeste] er alene nok til å avgjøre.
     *
     * Dette er ingen simulering av avgiftsgrunnlaget: den faktiske utvelgelsen filtrerer også på år og
     * periodeoverlapp, mens sammenligningen her er global maks mot global maks. `true` er en pålitelig
     * grunn til å stoppe; `false` er ikke et frikjenn. Derfor listes alle datoene.
     *
     * Saker uten kandidater er ikke med i lista — send N saksnummer og du kan få M ≤ N rader. Bruk
     * [VedtaksmetadataFiksResultat.utenMetadataPerSak] for å krysskontrollere.
     */
    private fun sorteringspåvirkning(saksnummer: List<String>): List<SorteringspåvirkningRad> {
        if (saksnummer.isEmpty()) return emptyList()

        val eksisterendePerSak = raderPerSak(EKSISTERENDE_NYESTE_SQL, saksnummer, medMarkoer = true)
        val kandidaterPerSak = raderPerSak(PATCH_NYESTE_SQL, saksnummer, medMarkoer = false)

        return kandidaterPerSak.keys.sorted().map { sak ->
            val eksisterende = eksisterendePerSak[sak].orEmpty()
            val nyesteKandidat = kandidaterPerSak.getValue(sak).first()

            fun datoer(opphav: Datoopphav) =
                eksisterende.filter { it.opphav == opphav }.mapNotNull { it.visning }

            // Rader uten dato holdes utenfor sammenligningen: ÅrsavregningService sorterer dem
            // som de eldste, så de kan aldri være den nyeste behandlingen patchen konkurrerer mot.
            val nyesteSammenlignbare = eksisterende
                .filter { it.opphav != Datoopphav.PATCHET_URØRT && it.sortering != null }
                .maxByOrNull { it.sortering!! }

            SorteringspåvirkningRad(
                saksnummer = sak,
                nyesteKandidatId = nyesteKandidat.behandlingsresultatId,
                nyesteKandidatDato = nyesteKandidat.visning,
                nyesteSammenlignbareId = nyesteSammenlignbare?.behandlingsresultatId,
                nyesteSammenlignbareDato = nyesteSammenlignbare?.visning,
                // >= og ikke >: ved eksakt likt tidsstempel er utfallet i den ekte sorteringen
                // vilkårlig (stabil sortering på behandlingsrekkefølgen), og et myntkast skal
                // flagges, ikke rapporteres som «patchen taper».
                patchenVinnerNyeste = nyesteSammenlignbare != null &&
                    requireNotNull(nyesteKandidat.sortering) >= nyesteSammenlignbare.sortering!!,
                patchenBlirNyesteIHeleSaken = eksisterende
                    .mapNotNull { it.sortering }
                    .maxOrNull()
                    ?.let { requireNotNull(nyesteKandidat.sortering) >= it } ?: true,
                ekteDatoer = datoer(Datoopphav.EKTE),
                usikreDatoer = datoer(Datoopphav.PATCHET_ENDRET),
                patchedeDatoer = datoer(Datoopphav.PATCHET_URØRT),
                antallUdaterteRader = eksisterende.count { it.sortering == null },
            )
        }
    }

    /**
     * Alle rader per sak, nyest først. Kandidatspørringen tar `.first()` for å finne raden patchen
     * skriver, og datolistene i rapporten arver rekkefølgen — begge avhenger av `ORDER BY` i SQL-ene.
     */
    @Suppress("UNCHECKED_CAST")
    private fun raderPerSak(sql: String, saksnummer: List<String>, medMarkoer: Boolean): Map<String, List<SortertRad>> {
        val query = entityManager.createNativeQuery(sql)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
        if (medMarkoer) query.setParameter("markoer", PATCH_MARKØR)

        return (query.resultList as List<Array<Any?>>)
            .map { rad ->
                SortertRad(
                    saksnummer = rad[0] as String,
                    behandlingsresultatId = (rad[1] as Number).toLong(),
                    visning = rad[2] as String?,
                    sortering = rad[3] as String?,
                    opphav = Datoopphav.av((rad[4] as Number).toInt()),
                )
            }
            .groupBy { it.saksnummer }
    }

    /**
     * Hvor datoen på en `vedtak_metadata`-rad kommer fra, utledet av auditfeltene: `registrert_av`
     * er `@CreatedBy` og settes kun ved insert, så den forteller pålitelig om raden ble laget av
     * fiksen. `endret_av` er `@LastModifiedBy` og flyttes av *enhver* senere skriving, så «rørt
     * siden» betyr ikke nødvendigvis «korrigert til ekte vedtaksdato» — derfor regnes
     * [PATCHET_ENDRET] konservativt med i seleunderlaget.
     */
    private enum class Datoopphav {
        /** Raden ble ikke laget av fiksen. Datoen er en ekte vedtaksdato. */
        EKTE,

        /** Laget av fiksen og urørt siden. Datoen er beviselig vår egen proxy. */
        PATCHET_URØRT,

        /** Laget av fiksen, men skrevet til siden. Kan være korrigert til en ekte dato — vi vet ikke. */
        PATCHET_ENDRET,
        ;

        companion object {
            fun av(kode: Int) = when (kode) {
                0 -> EKTE
                1 -> PATCHET_URØRT
                else -> PATCHET_ENDRET
            }
        }
    }

    /**
     * [visning] er lesbar for operatøren; [sortering] har mikrosekunder og er den som sammenlignes.
     * Begge er null når raden ikke har vedtaksdato — kolonnen er nullbar.
     */
    private data class SortertRad(
        val saksnummer: String,
        val behandlingsresultatId: Long,
        val visning: String?,
        val sortering: String?,
        val opphav: Datoopphav,
    )

    private fun hentAngreKandidater(saksnummer: List<String>): List<VedtaksmetadataAngreRad> {
        val sql = if (saksnummer.isEmpty()) ANGRE_KANDIDAT_SQL else ANGRE_KANDIDAT_SQL + ANGRE_KANDIDAT_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKØR)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)

        return (query.resultList as List<Array<Any?>>).map { rad ->
            VedtaksmetadataAngreRad(
                saksnummer = rad[0] as String,
                behandlingsresultatId = (rad[1] as Number).toLong(),
                vedtakDato = rad[2] as String?,
                vedtakType = rad[3] as String?,
            )
        }
    }

    private fun tellSomIkkeKanAngres(saksnummer: List<String>): Int {
        val sql = if (saksnummer.isEmpty()) KAN_IKKE_ANGRES_SQL else KAN_IKKE_ANGRES_SQL + ANGRE_KANDIDAT_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKØR)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)
        return (query.singleResult as Number).toInt()
    }

    /** Etterkontroll: hvor mange defekte rader står igjen per sak. Tom etter en vellykket skarp kjøring. */
    @Suppress("UNCHECKED_CAST")
    private fun tellUtenMetadata(saksnummer: List<String>): Map<String, Int> {
        if (saksnummer.isEmpty()) return emptyMap()

        val rader = entityManager.createNativeQuery(ETTERKONTROLL_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .resultList as List<Array<Any?>>

        return rader.associate { rad -> (rad[0] as String) to (rad[1] as Number).toInt() }
    }

    companion object {
        /**
         * Skrives i registrert_av/endret_av og er nøkkelen angreknappen sletter på. Verdien er en
         * del av datakontrakten mot prod — den står i rader fiksen allerede har satt inn — og kan
         * ikke endres.
         */
        const val PATCH_MARKØR = "MELOSYS-8174-PATCH"

        /**
         * Sakene fra fiksplanen som fag har bekreftet har trygdeavgift til Nav for 2024. Brukes kun
         * som default i preview — skarp kjøring krever eksplisitt liste. MEL-409394 er bevisst
         * utelatt: avklart separat (år-løs årsavregning, ikke manglende vedtaksmetadata).
         */
        val STANDARD_SAKER = listOf("MEL-448193", "MEL-545776", "MEL-632908")

        /** Maks antall saksnummer per kall. Holder oss også godt unna [MAKS_UTTRYKK_I_IN]. */
        const val MAKS_ANTALL_SAKER = 25

        /**
         * Oracles tak på antall uttrykk i en IN-liste (ORA-01795). `INSERT_SQL` binder kandidat-IDene
         * i `IN (:ider)`, og den lista er like lang som antall kandidatrader — ikke antall saker.
         * Uten selen ville en stor kjøring feilet med 500 midt i skrittet som endrer prod, i stedet
         * for en 400 operatøren kan handle på.
         */
        const val MAKS_UTTRYKK_I_IN = 1000

        /** Default tak på antall rader en skarp kjøring får sette inn. Kan heves eksplisitt i requesten. */
        const val STANDARD_MAKS_ANTALL_RADER = 10

        /**
         * Behandlingstyper vi trygt kan utlede vedtakstype for: FØRSTEGANG gir FØRSTEGANGSVEDTAK,
         * NY_VURDERING og ENDRET_PERIODE gir ENDRINGSVEDTAK (samme mønster som Flyway-patchen V7.6_04).
         * Andre typer — KLAGE, ANKE, SATSENDRING … — har egne vedtakstyper i kodeverket og avvises.
         */
        val KJENTE_BEH_TYPER = listOf("FØRSTEGANG", "NY_VURDERING", "ENDRET_PERIODE")

        /** Resultattypene ÅrsavregningService slår opp på — det er kun disse som kan velte en sak. */
        private val RESULTATTYPER = listOf("FASTSATT_TRYGDEAVGIFT", "FASTSATT_LOVVALGSLAND", "MEDLEM_I_FOLKETRYGDEN")

        /** Delt av [KANDIDAT_SQL] og [INSERT_SQL], slik at forhåndsvisningen treffer nøyaktig de samme radene. */
        private const val KANDIDAT_WHERE = """
            WHERE b.status = 'AVSLUTTET'
              AND br.resultat_type IN (:resultattyper)
              AND NOT EXISTS (SELECT 1 FROM vedtak_metadata vm WHERE vm.behandlingsresultat_id = br.behandling_id)
              AND b.saksnummer IN (:saksnummer)
        """

        /**
         * vedtak_type utledes av beh_type (kodeverkskoden er FØRSTEGANG, ikke FØRSTEGANGSBEHANDLING) —
         * samme utledning som Flyway-patchen V7.6_04. beh_type *bestemmer* strengt tatt ikke
         * vedtakstypen (den sendes separat i FattVedtakRequest), men feltet leses ikke av
         * ÅrsavregningService, og alternativet NULL er verre: A1TypeUtstedelse.av() switcher på
         * enumen uten null-gren.
         */
        private const val VEDTAK_TYPE_CASE =
            "CASE WHEN b.beh_type = 'FØRSTEGANG' THEN 'FØRSTEGANGSVEDTAK' ELSE 'ENDRINGSVEDTAK' END"

        /**
         * vedtak_dato er en proxy: den ekte vedtaksdatoen finnes ikke lenger, så vi bruker
         * behandlingsresultat.endret_dato. Klagefrist +42 dager følger Flyway-patchen V7.6_04.
         * Datoene formateres i SQL for å slippe JDBC-typemapping i rapporten.
         */
        private const val KANDIDAT_SQL = """
            SELECT b.saksnummer,
                   br.behandling_id,
                   b.beh_type,
                   br.resultat_type,
                   TO_CHAR(br.endret_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(TRUNC(CAST(br.endret_dato AS DATE)) + 42, 'YYYY-MM-DD'),
                   $VEDTAK_TYPE_CASE
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            ORDER BY b.saksnummer, br.endret_dato
        """

        private const val INSERT_SQL = """
            INSERT INTO vedtak_metadata
                (behandlingsresultat_id, vedtak_dato, vedtak_klagefrist, vedtak_type,
                 registrert_dato, endret_dato, registrert_av, endret_av)
            SELECT br.behandling_id,
                   br.endret_dato,
                   TRUNC(CAST(br.endret_dato AS DATE)) + 42,
                   $VEDTAK_TYPE_CASE,
                   SYSTIMESTAMP, SYSTIMESTAMP, :markoer, :markoer
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
              AND br.behandling_id IN (:ider)
        """

        /**
         * Radene som allerede har vedtaksmetadata — det patch-radene sammenlignes mot. Nyest først;
         * `behandling_id` som sekundærnøkkel gjør rekkefølgen deterministisk, og `NULLS LAST` fordi
         * Oracle ellers sorterer NULL først i `DESC` (udaterte rader lukes uansett ut i
         * [sorteringspåvirkning]). Siste kolonne er opphavskoden i [Datoopphav].
         *
         * To datokolonner: en lesbar til rapporten, og en med mikrosekunder til sammenligningen —
         * vedtak_dato er en TIMESTAMP som ÅrsavregningService sammenligner i full oppløsning, og
         * uten mikrosekundene ser «samme sekund» ut som «patchen vinner ikke».
         */
        private const val EKSISTERENDE_NYESTE_SQL = """
            SELECT b.saksnummer,
                   br.behandling_id,
                   TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD HH24:MI:SS.FF6'),
                   CASE
                       WHEN vm.registrert_av IS NULL OR vm.registrert_av <> :markoer THEN 0
                       WHEN vm.endret_av = :markoer THEN 1
                       ELSE 2
                   END
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
            WHERE b.status = 'AVSLUTTET'
              AND br.resultat_type IN (:resultattyper)
              AND b.saksnummer IN (:saksnummer)
            ORDER BY b.saksnummer, vm.vedtak_dato DESC NULLS LAST, br.behandling_id DESC
        """

        /** Kandidatene, med datoen patchen faktisk ville skrevet. Nyest først. */
        private const val PATCH_NYESTE_SQL = """
            SELECT b.saksnummer,
                   br.behandling_id,
                   TO_CHAR(br.endret_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(br.endret_dato, 'YYYY-MM-DD HH24:MI:SS.FF6'),
                   0
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            ORDER BY b.saksnummer, br.endret_dato DESC, br.behandling_id DESC
        """

        private const val ETTERKONTROLL_SQL = """
            SELECT b.saksnummer, COUNT(*)
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            GROUP BY b.saksnummer
        """

        /** Kun rader som fortsatt er urørte: endret_av flyttes av enhver senere skriving (@LastModifiedBy). */
        private const val ANGRE_KANDIDAT_SQL = """
            SELECT b.saksnummer,
                   vm.behandlingsresultat_id,
                   TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   vm.vedtak_type
            FROM vedtak_metadata vm
            JOIN behandling b ON b.id = vm.behandlingsresultat_id
            WHERE vm.registrert_av = :markoer
              AND vm.endret_av = :markoer
        """
        private const val ANGRE_KANDIDAT_SAKSFILTER = " AND b.saksnummer IN (:saksnummer)"

        /**
         * Patch-rader som ikke lenger er urørte og som [ANGRE_SQL] derfor lar stå — enten er ekte
         * data skrevet oppå, eller markøren er tømt. `endret_av` er nullbar, og i Oracle er både
         * `= :markoer` og `<> :markoer` UNKNOWN mot NULL: uten NULL-grenen faller en slik rad ut av
         * BÅDE angre-kandidatene og denne tellingen, og svaret ser ut som «ingenting å angre» i
         * stedet for «én rad kunne ikke rulles tilbake».
         */
        private const val KAN_IKKE_ANGRES_SQL = """
            SELECT COUNT(*)
            FROM vedtak_metadata vm
            JOIN behandling b ON b.id = vm.behandlingsresultat_id
            WHERE vm.registrert_av = :markoer
              AND (vm.endret_av IS NULL OR vm.endret_av <> :markoer)
        """

        private const val ANGRE_SQL = """
            DELETE FROM vedtak_metadata
            WHERE registrert_av = :markoer
              AND endret_av = :markoer
        """
        private const val ANGRE_SAKSFILTER = """
              AND behandlingsresultat_id IN (SELECT b.id FROM behandling b WHERE b.saksnummer IN (:saksnummer))
        """
    }
}

data class VedtaksmetadataFiksRad(
    val saksnummer: String,
    val behandlingsresultatId: Long,
    val behType: String?,
    val resultatType: String?,
    val blirVedtakDato: String?,
    val blirKlagefrist: String?,
    val blirVedtakType: String,
)

/**
 * Hva patchen gjør med vedtaksdato-sorteringen for én sak. [patchenVinnerNyeste] er det eneste
 * feltet som blokkerer skarp kjøring; resten er rapport. Summen av de tre datolistene er alle
 * daterte rader `ÅrsavregningService` ser i saken — samme avgrensning som
 * `hentGjeldendeBehandlingsresultaterForÅrsavregning` (rader på åpne behandlinger er derfor ikke med).
 */
data class SorteringspåvirkningRad(
    val saksnummer: String,
    /** Raden patchen vil skrive — den nyeste av kandidatene i saken, med datoen den får. */
    val nyesteKandidatId: Long,
    val nyesteKandidatDato: String?,
    /**
     * Nyeste daterte rad som *kan* være et ekte vedtak ([ekteDatoer] og [usikreDatoer]). Null betyr
     * at saken ikke har noen slik rad — da er det ingenting å fortrenge, og selen slipper kjøringen
     * gjennom.
     */
    val nyesteSammenlignbareId: Long?,
    val nyesteSammenlignbareDato: String?,
    /**
     * Patchen legger seg på eller over [nyesteSammenlignbareDato], altså kan den fortrenge et vedtak
     * som er ekte. True også ved eksakt likt tidsstempel — da er utfallet vilkårlig, og det skal
     * flagges. **Blokkerer skarp kjøring med mindre saksnummeret er kvittert ut.**
     */
    val patchenVinnerNyeste: Boolean,
    /**
     * Patchen blir nyeste rad i saken sett mot ALLE rader, også de vi selv satte inn tidligere.
     * Blokkerer ikke — fortrenger den bare en tidligere patch, er det ingen ekte dato som ryker — men
     * kjøringen endrer likevel hvilken behandling `ÅrsavregningService` regner som nyest, så den logges.
     */
    val patchenBlirNyesteIHeleSaken: Boolean,
    /**
     * Datoer fra rader denne fiksen ikke har laget. Ikke en garanti for ekthet: Flyway-patchen
     * `V7.6_04__patch_vedtak_metadata_endret_periode` skrev samme endret_dato-proxy uten markør, så
     * rader derfra havner her og ser ekte ut. Å telle dem med er den konservative retningen for selen.
     */
    val ekteDatoer: List<String> = emptyList(),
    /**
     * Datoer fra rader fiksen laget, men som noen har skrevet til siden. Kan være korrigert til en
     * ekte vedtaksdato — vi kan ikke vite. Regnes med i seleunderlaget, altså konservativt.
     */
    val usikreDatoer: List<String> = emptyList(),
    /** Datoer fra rader fiksen laget og ingen har rørt. Beviselig vår egen proxy — styrer ingenting. */
    val patchedeDatoer: List<String> = emptyList(),
    /**
     * Rader med vedtaksmetadata men uten `vedtak_dato`. De sorterer eldst og teller ikke som
     * sammenligningsgrunnlag, men uten feltet leses tre tomme datolister som «saken har ingen
     * vedtaksmetadata» når den kan ha flere.
     */
    val antallUdaterteRader: Int = 0,
)

data class VedtaksmetadataFiksResultat(
    val skarp: Boolean,
    val saksnummer: List<String>,
    val antallRaderFunnet: Int,
    val antallRaderInnsatt: Int,
    /** Kandidatene fra preview-spørringen. Ved `avvik = true` er dette ikke det samme som det som ble skrevet. */
    val rader: List<VedtaksmetadataFiksRad>,
    /** Etterkontroll, per sak: rader uten vedtaksmetadata slik det står nå. Tom = ingen igjen. */
    val utenMetadataPerSak: Map<String, Int>,
    /** Kandidater der vedtakstypen ikke kan utledes trygt. Blokkerer skarp kjøring. */
    val ukjentBehType: List<Long> = emptyList(),
    /** Saksnummer fra requesten uten kandidatrader — skrivefeil, eller sak uten defekte rader. Blokkerer ikke. */
    val saksnummerUtenKandidater: List<String> = emptyList(),
    /** Kvitteringer i `tillatSorteringsendring` som ikke traff noen kaprende sak — typisk en skrivefeil. Blokkerer ikke. */
    val kvitteringerUtenTreff: List<String> = emptyList(),
    /** True hvis antall innsatte rader ikke stemmer med forhåndsvisningen. */
    val avvik: Boolean = false,
    /** Per sak: bytter patchen ut hvilken behandling som er nyest i vedtaksdato-sorteringen? */
    val sorteringspåvirkning: List<SorteringspåvirkningRad> = emptyList(),
    val markoer: String = VedtaksmetadataFiksService.PATCH_MARKØR,
    val angreEndepunkt: String = "POST /admin/aarsavregninger/saker/skattepliktige/vedtaksmetadata-fiks/angre",
)

data class VedtaksmetadataAngreRad(
    val saksnummer: String,
    val behandlingsresultatId: Long,
    val vedtakDato: String?,
    val vedtakType: String?,
)

data class VedtaksmetadataAngreResultat(
    val skarp: Boolean,
    /** Tom liste = alle markerte rader, uansett sak. */
    val saksnummer: List<String>,
    val antallRaderFunnet: Int,
    val antallSlettet: Int,
    val rader: List<VedtaksmetadataAngreRad>,
    /** Patch-rader som er endret etterpå (ekte data skrevet oppå) — disse røres aldri. */
    val antallSomIkkeKanAngres: Int,
    val avvik: Boolean = false,
    val markoer: String = VedtaksmetadataFiksService.PATCH_MARKØR,
)
