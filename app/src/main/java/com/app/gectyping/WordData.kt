package com.app.gectyping

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets

data class WordWithSynonyms(
    val word: String,
    val synonyms: List<String>
)

object WordList {
    private fun parseWords(raw: String): List<WordWithSynonyms> {
        return raw
            .replace("\uFEFF", "")           // strip BOM (causes "LE" artefact in TTS)
            .replace("\u200B", "")           // zero-width space
            .replace("\u200C", "")           // zero-width non-joiner
            .replace("\u200D", "")           // zero-width joiner
            .replace("\u00AD", "")           // soft hyphen
            .replace("\u060c", ",")
            .split("\n", ",")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.removeSuffix(".") }
            .map { it.trim() }               // trim again after suffix removal
            .filter { it.isNotBlank() }
            .map { WordWithSynonyms(it.lowercase(), emptyList()) }
            .distinctBy { it.word }
            .toList()
    }

    private val level1Raw = """
cash, debit, credit card, cheque, advance, fee, monthly, membership, interest, deposit, fees, poverty, bank, statement, money, management, current, account, student, withdraw, low, risk, investment, mortgage, grace, period, budget, voucher, coupon, public, taxpayers, debt, credit, purchase, refund, annuity, non, refundable, card, distribution, costs, income, finance, department, family, finances, duty, free, store
""".trimIndent()

    private val level2Raw = """
science, politics, history, biology, architecture, law, geography, literature, business, management, agriculture, statistics, mathematics, logic, physics, psychology, economics, philosophy, arts, chemistry, humanities, course, outline, group, discussion, handout, written, work, report, research, proofreading, experiment, experience, reference, textbook, dictionary, laptop, printer, advisor, teamwork, module, topic, assessment, library, department, computer, centre, classroom, lecture, tutor, hall, attendance, deadline, talk, speech, laboratory, certificate, diploma, test, students, full, time, facilities, college, dining, room, specialist, knowledge, international, accommodation, home, stay, primary, secondary, intermediate, media, resources, staff, commencement, dissertation, leaflet, faculty, pupils, pencil, feedback, tasks, outcomes, advanced, introductory, background, higher, education, guidelines, post, secondary, supervisor, bachelor, degree, compound, vocabulary, support, services, retention, publication, foreign, schedule, school, reunion, registrar, office, stationery
""".trimIndent()

    private val level3Raw = """
catalogue, interview, newsletter, competition, program, strategies, research, method, entertainment, industry, leadership, management, display, products, customer, special, offer, collecting, data, questionnaire, survey, mass, media, statistic, profit, margin, poll, business, card, training, trainee, merchandise, manufacture, recruitment, yoga, keep, fit, salad, bar, vegetarian, outdoor, activities, leisure, time, disease, meal, protein, balanced, diet, food, pyramid, vitamin, carbohydrates, rice, pasta, potatoes, pizza, tomatoes, bread, cereals, minerals, zinc, meat, seafood, eggs, beans, milk, cheese, yoghurt, fruit, vegetables, citrus, fruits, green, pepper, nuts, egg, yolk, liver, medicine, treatment, remedy, nursing, care, nursery, regular, exercise
""".trimIndent()

    private val level4Raw = """
field, footbridge, environment, waterfall, river, mountain, forest, village, coast, reef, lake, valley, hill, cliff, island, peninsula, earthquake, avalanche, tornado, typhoon, desertification, volcano, disaster, catastrophe, erosion, landslide, storm, flood, hurricane, pond, jungle, oasis, dam, canyon, greenhouse, effect, acid, rain, global, warming, carbon, dioxide, burning, fossil, exhaust, fumes, deforestation, nitrogen, oxide, smog, climate, pollution, temperature, power, plants, landfill, cattle, wind, turbine, solar, power, hydroelectric, renewable, source, energy, reliable, panels, environmentally, friendly, oxygen, chemical, free, degradation, vegetation, sea, level, ocean, currents, soil, conditioner, coal, fuels, firewood, drought, contaminated
""".trimIndent()

    private val level5Raw = """
birds, of, prey, seabirds, poultry, and, game, mammals, cetacean, whale, primates, rodents, fish, amphibian, reptile, insects, octopus, phylum, class, order, family, genus, species, livestock, creature, lion, penguin, mushroom, fungus, leaves, seed, core, bark, trunk, twig, branch, flower, stem, roots, cluster, fertilizer, south, america, north, africa, asia, europe, australia, antarctica, egypt, mexico, france, indonesia, turkey, england, germany, china, greece, brazil, india, korea, malaysia, new, zealand, nigeria, pakistan, singapore, switzerland, kingdom, italy, dominican, republic, philippines, denmark, linguistics, bilingual, trilingual, polyglot, portuguese, mandarin, bengali, chinese, hindi, russian, japanese, german, punjabi, thai, persian, filipino, french, italian, greek
""".trimIndent()

    private val level6Raw = """
dome, palace, fort, castle, glasshouse, pyramid, log, cabin, lighthouse, hut, skyscraper, sculpture, semi, detached, house, duplex, terraced, town, row, bungalow, thatched, cottage, mobile, home, houseboat, block, of, flats, apartment, building, condominium, chimney, bedroom, basement, landlord, tenant, rent, lease, neighborhood, suburb, sofa, coffee, table, dormitory, storey, kitchen, refrigerator, microwave, ground, floor, oven, hallway, insurance, cities, street, lane, city, centre, central, station, car, park, department, store, bridge, temple, embassy, road, system, hospital, garden, avenue, clinic, dentist, reception, appointment, staff, selection, colleague, workshop, showroom, information, desk, employer, employment, unemployed, technical, cooperation, team, leaders, stress, ability, vision, confidence, employee, internship
""".trimIndent()

    private val level7Raw = """
reasonable, satisfactory, dangerous, safe, strongly, recommended, poor, quality, satisfied, disappointed, efficient, luxurious, colored, spotted, striped, expensive, cheap, tourist, guided, tour, ticket, office, souvenir, trip, guest, reservation, view, culture, memorable, single, double, bedded, room, picnic, attraction, hostel, suite, aquarium, train, develop, collect, supervise, mark, edit, revise, exhibit, donate, surpass, register, support, hunt, persuade, concentrate, discuss, suggest, arrange, borrow, immigrate, review, learn, touch, energetic, social, ancient, necessary, fantastic, exciting, fabulous, dull, comfortable, convenient, suitable, affordable, voluntary, mandatory, compulsory, temporary, permanent, immense, vast, salty, extinct, vulnerable, pessimistic, optimistic, realistic, practical, knowledgeable, flexible, confident, western, intensive, tranquil, spectacular, intact, various, orienteering, caving, spelunking, archery, skating, diving, snorkeling, skateboarding, bowls, darts, golf, billiards, photography, painting, pottery, woodcarving, gardening, stamp, collection, embroidery, climbing, chess, parachute
""".trimIndent()

    private val levelWords: Map<Int, List<WordWithSynonyms>> = mapOf(
        1 to parseWords(level1Raw),
        2 to parseWords(level2Raw),
        3 to parseWords(level3Raw),
        4 to parseWords(level4Raw),
        5 to parseWords(level5Raw),
        6 to parseWords(level6Raw),
        7 to parseWords(level7Raw)
    )

    fun wordsForLevel(level: Int): List<WordWithSynonyms> {
        return levelWords[level.coerceIn(1, 7)] ?: emptyList()
    }

    fun getWordCountForLevel(level: Int): Int {
        return wordsForLevel(level).size
    }
}

object SynonymRepository {
    private const val PREFS_NAME = "synonyms_cache"

    suspend fun getOrFetchFour(context: Context, word: String): List<String> {
        val cleaned = word.trim()
        if (cleaned.isEmpty()) return emptyList()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "syn_${cleaned.lowercase()}"
        val cached = prefs.getString(key, null)
        if (!cached.isNullOrBlank()) {
            return cached.split('|').map { it.trim() }.filter { it.isNotBlank() }.take(4)
        }

        val fetched = fetchFromDatamuse(cleaned).take(4)
        if (fetched.isNotEmpty()) {
            prefs.edit().putString(key, fetched.joinToString("|")) .apply()
        }
        return fetched
    }

    private suspend fun fetchFromDatamuse(word: String): List<String> = withContext(Dispatchers.IO) {
        val url = URL("https://api.datamuse.com/words?ml=${URLEncoder.encode(word, StandardCharsets.UTF_8.name())}&max=12")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000  // Reduced from 7s — don't block gameplay for synonyms
            readTimeout = 4000
            requestMethod = "GET"
        }

        try {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            val out = LinkedHashSet<String>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val w = obj.optString("word")
                    .replace("\uFEFF", "").trim()  // Strip BOM from API responses too
                if (w.isNotEmpty() && !w.equals(word, ignoreCase = true)) {
                    out.add(w)
                }
                if (out.size >= 4) break
            }
            out.toList()
        } catch (e: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }
}
