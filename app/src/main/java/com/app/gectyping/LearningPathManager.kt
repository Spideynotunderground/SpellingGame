package com.app.gectyping

import android.content.Context
import android.content.SharedPreferences

/**
 * ============================================================================
 * LEARNING PATH SYSTEM — Duolingo-style learning journey
 * 
 * Features:
 * - Units organized by themes (Animals, Food, Travel, etc.)
 * - Lessons within each unit (5-10 words per lesson)
 * - Progress tracking per lesson
 * - Crown system (0-5 crowns per lesson)
 * - Unit tests (boss levels)
 * - Skill decay and practice mode
 * ============================================================================
 */

data class WordItem(
    val word: String,
    val translation: String = "",
    val example: String = "",
    val difficulty: Int = 1 // 1-5
)

data class Lesson(
    val id: String,
    val title: String,
    val words: List<WordItem>,
    val iconEmoji: String = "📚"
)

data class LearningUnit(
    val id: String,
    val title: String,
    val description: String,
    val lessons: List<Lesson>,
    val iconEmoji: String,
    val color: Long, // ARGB color
    val requiredXP: Int = 0 // XP needed to unlock this unit
)

data class LessonProgress(
    val lessonId: String,
    val crowns: Int = 0, // 0-5 crowns
    val wordsLearned: Int = 0,
    val totalWords: Int = 0,
    val lastPracticed: Long = 0,
    val strength: Float = 1f, // 0-1, decays over time
    val perfectCompletions: Int = 0
)

data class LearningUnitProgress(
    val unitId: String,
    val lessonsCompleted: Int = 0,
    val totalLessons: Int = 0,
    val unitTestPassed: Boolean = false,
    val isUnlocked: Boolean = false
)

data class LearningPathState(
    val currentUnitIndex: Int = 0,
    val currentLessonIndex: Int = 0,
    val totalXP: Int = 0,
    val unitsProgress: Map<String, LearningUnitProgress> = emptyMap(),
    val lessonsProgress: Map<String, LessonProgress> = emptyMap()
)

// Crown levels and their requirements
enum class CrownLevel(val level: Int, val requiredCompletions: Int, val xpBonus: Int) {
    NONE(0, 0, 0),
    BRONZE(1, 1, 10),
    SILVER(2, 2, 15),
    GOLD(3, 3, 20),
    PLATINUM(4, 4, 25),
    LEGENDARY(5, 5, 50)
}

object LearningPathData {
    
    // ========== BEGINNER LEVEL ==========
    
    // TOPIC 1: EDUCATION
    private val educationLessons = listOf(
        Lesson(id = "education_1", title = "Education 1", iconEmoji = "📚", words = listOf(
            WordItem("curriculum", "учебная программа"), WordItem("syllabus", "учебный план"), WordItem("assessment", "оценка"), WordItem("coursework", "курсовая работа"),
            WordItem("assignment", "задание"), WordItem("dissertation", "диссертация"), WordItem("tuition fees", "плата за обучение"), WordItem("scholarship", "стипендия"),
            WordItem("grant", "грант"), WordItem("undergraduate", "бакалавр")
        )),
        Lesson(id = "education_2", title = "Education 2", iconEmoji = "📚", words = listOf(
            WordItem("postgraduate", "аспирант"), WordItem("vocational training", "профессиональное обучение"), WordItem("distance learning", "дистанционное обучение"), WordItem("academic performance", "успеваемость"),
            WordItem("literacy", "грамотность"), WordItem("numeracy", "умение считать"), WordItem("compulsory education", "обязательное образование"), WordItem("higher education", "высшее образование"),
            WordItem("state school", "государственная школа"), WordItem("private institution", "частное учреждение")
        )),
        Lesson(id = "education_3", title = "Education 3", iconEmoji = "📚", words = listOf(
            WordItem("extracurricular activities", "внеклассные занятия"), WordItem("enrolment", "зачисление"), WordItem("deadline", "крайний срок"), WordItem("seminar", "семинар"),
            WordItem("lecture hall", "лекционный зал"), WordItem("supervisor", "научный руководитель"), WordItem("research paper", "научная работа"), WordItem("citation", "цитирование"),
            WordItem("plagiarism", "плагиат"), WordItem("qualification", "квалификация")
        )),
        Lesson(id = "education_4", title = "Education 4", iconEmoji = "📚", words = listOf(
            WordItem("diploma", "диплом"), WordItem("degree", "степень"), WordItem("foundation course", "подготовительный курс"), WordItem("academic year", "учебный год"),
            WordItem("gap year", "академический отпуск"), WordItem("student loan", "студенческий кредит"), WordItem("internship", "стажировка"), WordItem("peer review", "рецензирование"),
            WordItem("academic achievement", "академическое достижение"), WordItem("discipline", "дисциплина")
        )),
        Lesson(id = "education_5", title = "Education 5", iconEmoji = "📚", words = listOf(
            WordItem("faculty", "факультет"), WordItem("tutorial", "консультация"), WordItem("grading system", "система оценок"), WordItem("thesis statement", "тезис"),
            WordItem("methodology", "методология"), WordItem("critical thinking", "критическое мышление"), WordItem("revision", "повторение"), WordItem("workload", "учебная нагрузка"),
            WordItem("attendance", "посещаемость"), WordItem("certification", "сертификация")
        ))
    )
    
    // TOPIC 2: ACCOMMODATION
    private val accommodationLessons = listOf(
        Lesson(id = "accommodation_1", title = "Accommodation 1", iconEmoji = "🏠", words = listOf(
            WordItem("mortgage", "ипотека"), WordItem("tenancy", "аренда"), WordItem("landlord", "арендодатель"), WordItem("tenant", "арендатор"),
            WordItem("lease agreement", "договор аренды"), WordItem("utilities", "коммунальные услуги"), WordItem("deposit", "залог"), WordItem("rent increase", "повышение арендной платы"),
            WordItem("furnished", "меблированный"), WordItem("unfurnished", "без мебели")
        )),
        Lesson(id = "accommodation_2", title = "Accommodation 2", iconEmoji = "🏠", words = listOf(
            WordItem("maintenance", "обслуживание"), WordItem("renovation", "ремонт"), WordItem("suburb", "пригород"), WordItem("residential area", "жилой район"),
            WordItem("accommodation shortage", "нехватка жилья"), WordItem("property market", "рынок недвижимости"), WordItem("estate agent", "риелтор"), WordItem("housing estate", "жилой комплекс"),
            WordItem("detached house", "отдельный дом"), WordItem("semi-detached", "двухквартирный дом")
        )),
        Lesson(id = "accommodation_3", title = "Accommodation 3", iconEmoji = "🏠", words = listOf(
            WordItem("terraced house", "рядный дом"), WordItem("high-rise building", "высотное здание"), WordItem("studio flat", "квартира-студия"), WordItem("shared accommodation", "совместное жильё"),
            WordItem("affordable housing", "доступное жильё"), WordItem("council housing", "муниципальное жильё"), WordItem("eviction", "выселение"), WordItem("household bills", "бытовые счета"),
            WordItem("security system", "система безопасности"), WordItem("soundproof", "звукоизоляция")
        )),
        Lesson(id = "accommodation_4", title = "Accommodation 4", iconEmoji = "🏠", words = listOf(
            WordItem("central heating", "центральное отопление"), WordItem("insulation", "утепление"), WordItem("overcrowded", "переполненный"), WordItem("amenities", "удобства"),
            WordItem("relocation", "переезд"), WordItem("homeownership", "владение жильём"), WordItem("accommodation crisis", "жилищный кризис"), WordItem("living conditions", "условия жизни"),
            WordItem("spacious", "просторный"), WordItem("cramped", "тесный")
        )),
        Lesson(id = "accommodation_5", title = "Accommodation 5", iconEmoji = "🏠", words = listOf(
            WordItem("ventilation", "вентиляция"), WordItem("balcony", "балкон"), WordItem("basement", "подвал"), WordItem("attic", "чердак"),
            WordItem("property value", "стоимость недвижимости"), WordItem("real estate", "недвижимость"), WordItem("infrastructure", "инфраструктура"), WordItem("neighbourhood", "район"),
            WordItem("relocation expenses", "расходы на переезд"), WordItem("housing", "жильё")
        ))
    )
    
    // TOPIC 3: FOOD
    private val foodLessons = listOf(
        Lesson(id = "food_1", title = "Food 1", iconEmoji = "🍽", words = listOf(
            WordItem("nutritional value", "пищевая ценность"), WordItem("processed food", "обработанная еда"), WordItem("home-cooked meal", "домашняя еда"), WordItem("organic produce", "органические продукты"),
            WordItem("artificial additives", "искусственные добавки"), WordItem("preservatives", "консерванты"), WordItem("balanced diet", "сбалансированная диета"), WordItem("calorie intake", "потребление калорий"),
            WordItem("portion size", "размер порции"), WordItem("saturated fat", "насыщенные жиры")
        )),
        Lesson(id = "food_2", title = "Food 2", iconEmoji = "🍽", words = listOf(
            WordItem("sugar intake", "потребление сахара"), WordItem("whole grains", "цельные злаки"), WordItem("plant-based diet", "растительная диета"), WordItem("food poisoning", "пищевое отравление"),
            WordItem("food safety", "безопасность пищи"), WordItem("expiry date", "срок годности"), WordItem("takeaway meal", "еда на вынос"), WordItem("food chain", "пищевая цепь"),
            WordItem("dietary restrictions", "диетические ограничения"), WordItem("food labelling", "маркировка продуктов")
        )),
        Lesson(id = "food_3", title = "Food 3", iconEmoji = "🍽", words = listOf(
            WordItem("obesity rate", "уровень ожирения"), WordItem("malnutrition", "недоедание"), WordItem("metabolism", "обмен веществ"), WordItem("immune system", "иммунная система"),
            WordItem("culinary skills", "кулинарные навыки"), WordItem("cooking methods", "способы приготовления"), WordItem("boiling", "варка"), WordItem("steaming", "приготовление на пару"),
            WordItem("grilling", "гриль"), WordItem("frying", "жарка")
        )),
        Lesson(id = "food_4", title = "Food 4", iconEmoji = "🍽", words = listOf(
            WordItem("seasoning", "приправа"), WordItem("ingredients", "ингредиенты"), WordItem("staple food", "основной продукт"), WordItem("fast food consumption", "потребление фастфуда"),
            WordItem("eating habits", "пищевые привычки"), WordItem("food waste", "пищевые отходы"), WordItem("agriculture", "сельское хозяйство"), WordItem("local produce", "местные продукты"),
            WordItem("food industry", "пищевая промышленность"), WordItem("organic farming", "органическое земледелие")
        )),
        Lesson(id = "food_5", title = "Food 5", iconEmoji = "🍽", words = listOf(
            WordItem("genetically modified", "генетически модифицированный"), WordItem("supply chain", "цепочка поставок"), WordItem("food shortage", "нехватка продовольствия"), WordItem("food security", "продовольственная безопасность"),
            WordItem("undernourished", "недоедающий"), WordItem("appetite", "аппетит"), WordItem("digestion", "пищеварение"), WordItem("contamination", "загрязнение"),
            WordItem("nutrition", "питание"), WordItem("healthy eating", "здоровое питание")
        ))
    )
    
    // TOPIC 4: SHOPPING
    private val shoppingLessons = listOf(
        Lesson(id = "shopping_1", title = "Shopping 1", iconEmoji = "🛍", words = listOf(
            WordItem("consumer behaviour", "поведение потребителя"), WordItem("retail outlet", "торговая точка"), WordItem("online purchase", "онлайн-покупка"), WordItem("discount", "скидка"),
            WordItem("refund policy", "политика возврата"), WordItem("impulse buying", "импульсивная покупка"), WordItem("brand loyalty", "лояльность к бренду"), WordItem("counterfeit goods", "поддельные товары"),
            WordItem("warranty", "гарантия"), WordItem("receipt", "чек")
        )),
        Lesson(id = "shopping_2", title = "Shopping 2", iconEmoji = "🛍", words = listOf(
            WordItem("bulk buying", "оптовая покупка"), WordItem("window shopping", "разглядывание витрин"), WordItem("competitive pricing", "конкурентные цены"), WordItem("shopping mall", "торговый центр"),
            WordItem("customer service", "обслуживание клиентов"), WordItem("bargain", "выгодная сделка"), WordItem("luxury goods", "предметы роскоши"), WordItem("expenditure", "расходы"),
            WordItem("price comparison", "сравнение цен"), WordItem("overconsumption", "чрезмерное потребление")
        )),
        Lesson(id = "shopping_3", title = "Shopping 3", iconEmoji = "🛍", words = listOf(
            WordItem("retail therapy", "шопинг-терапия"), WordItem("second-hand goods", "подержанные товары"), WordItem("ethical shopping", "этичный шопинг"), WordItem("product range", "ассортимент"),
            WordItem("availability", "наличие"), WordItem("exchange policy", "политика обмена"), WordItem("delivery fee", "стоимость доставки"), WordItem("consumer rights", "права потребителя"),
            WordItem("market demand", "рыночный спрос"), WordItem("sales promotion", "стимулирование продаж")
        )),
        Lesson(id = "shopping_4", title = "Shopping 4", iconEmoji = "🛍", words = listOf(
            WordItem("advertising campaign", "рекламная кампания"), WordItem("loyalty card", "карта лояльности"), WordItem("instalment plan", "рассрочка"), WordItem("seasonal sale", "сезонная распродажа"),
            WordItem("clearance sale", "ликвидация товара"), WordItem("supplier", "поставщик"), WordItem("wholesaler", "оптовик"), WordItem("retailer", "розничный продавец"),
            WordItem("pricing strategy", "ценовая стратегия"), WordItem("shopping habits", "покупательские привычки")
        )),
        Lesson(id = "shopping_5", title = "Shopping 5", iconEmoji = "🛍", words = listOf(
            WordItem("product quality", "качество товара"), WordItem("limited edition", "ограниченный тираж"), WordItem("stock shortage", "дефицит товара"), WordItem("purchasing power", "покупательная способность"),
            WordItem("return policy", "политика возврата"), WordItem("online retailer", "интернет-магазин"), WordItem("doorstep delivery", "доставка на дом"), WordItem("pre-order", "предзаказ"),
            WordItem("promotional offer", "рекламное предложение"), WordItem("customer satisfaction", "удовлетворённость клиента")
        ))
    )
    
    // TOPIC 5: WEATHER
    private val weatherLessons = listOf(
        Lesson(id = "weather_1", title = "Weather 1", iconEmoji = "🌤", words = listOf(
            WordItem("forecast", "прогноз"), WordItem("humidity", "влажность"), WordItem("precipitation", "осадки"), WordItem("drought", "засуха"),
            WordItem("heatwave", "жара"), WordItem("cold spell", "похолодание"), WordItem("climate conditions", "климатические условия"), WordItem("temperature drop", "падение температуры"),
            WordItem("thunderstorm", "гроза"), WordItem("lightning strike", "удар молнии")
        )),
        Lesson(id = "weather_2", title = "Weather 2", iconEmoji = "🌤", words = listOf(
            WordItem("flood", "наводнение"), WordItem("snowfall", "снегопад"), WordItem("strong winds", "сильный ветер"), WordItem("overcast", "пасмурно"),
            WordItem("clear skies", "ясное небо"), WordItem("freezing point", "точка замерзания"), WordItem("mild weather", "мягкая погода"), WordItem("seasonal change", "смена сезонов"),
            WordItem("climate change", "изменение климата"), WordItem("extreme weather", "экстремальная погода")
        )),
        Lesson(id = "weather_3", title = "Weather 3", iconEmoji = "🌤", words = listOf(
            WordItem("hurricane", "ураган"), WordItem("tornado", "торнадо"), WordItem("rainfall", "количество осадков"), WordItem("weather patterns", "погодные модели"),
            WordItem("global warming", "глобальное потепление"), WordItem("carbon emissions", "выбросы углерода"), WordItem("greenhouse effect", "парниковый эффект"), WordItem("rising temperatures", "рост температур"),
            WordItem("coastal flooding", "прибрежное затопление"), WordItem("natural disaster", "стихийное бедствие")
        )),
        Lesson(id = "weather_4", title = "Weather 4", iconEmoji = "🌤", words = listOf(
            WordItem("weather warning", "штормовое предупреждение"), WordItem("storm surge", "штормовой нагон"), WordItem("drought-resistant", "засухоустойчивый"), WordItem("humid climate", "влажный климат"),
            WordItem("arid region", "засушливый регион"), WordItem("unpredictable weather", "непредсказуемая погода"), WordItem("sunny intervals", "солнечные промежутки"), WordItem("chilly breeze", "прохладный ветерок"),
            WordItem("tropical climate", "тропический климат"), WordItem("climate zone", "климатическая зона")
        )),
        Lesson(id = "weather_5", title = "Weather 5", iconEmoji = "🌤", words = listOf(
            WordItem("heavy downpour", "сильный ливень"), WordItem("air pressure", "атмосферное давление"), WordItem("seasonal variation", "сезонные колебания"), WordItem("weather conditions", "погодные условия"),
            WordItem("environmental impact", "воздействие на окружающую среду"), WordItem("heat exhaustion", "тепловое истощение"), WordItem("icy roads", "обледенелые дороги"), WordItem("foggy conditions", "туманные условия"),
            WordItem("scorching heat", "палящая жара"), WordItem("temperature", "температура")
        ))
    )
    
    // ========== UPPER-INTERMEDIATE LEVEL ==========
    
    // TOPIC 6: WORK
    private val workLessons = listOf(
        Lesson(id = "work_1", title = "Work 1", iconEmoji = "💼", words = listOf(
            WordItem("employment rate", "уровень занятости"), WordItem("unemployment", "безработица"), WordItem("job security", "гарантия работы"), WordItem("career prospects", "карьерные перспективы"),
            WordItem("promotion", "повышение"), WordItem("redundancy", "сокращение"), WordItem("workload", "рабочая нагрузка"), WordItem("deadline pressure", "давление сроков"),
            WordItem("work-life balance", "баланс работы и жизни"), WordItem("job satisfaction", "удовлетворённость работой")
        )),
        Lesson(id = "work_2", title = "Work 2", iconEmoji = "💼", words = listOf(
            WordItem("salary raise", "повышение зарплаты"), WordItem("benefits package", "социальный пакет"), WordItem("pension scheme", "пенсионная программа"), WordItem("recruitment process", "процесс найма"),
            WordItem("job vacancy", "вакансия"), WordItem("qualifications", "квалификация"), WordItem("professional development", "профессиональное развитие"), WordItem("training programme", "программа обучения"),
            WordItem("career ladder", "карьерная лестница"), WordItem("managerial position", "руководящая должность")
        )),
        Lesson(id = "work_3", title = "Work 3", iconEmoji = "💼", words = listOf(
            WordItem("leadership skills", "лидерские навыки"), WordItem("teamwork", "командная работа"), WordItem("conflict resolution", "разрешение конфликтов"), WordItem("workplace environment", "рабочая среда"),
            WordItem("corporate culture", "корпоративная культура"), WordItem("flexible hours", "гибкий график"), WordItem("remote work", "удалённая работа"), WordItem("part-time employment", "частичная занятость"),
            WordItem("full-time position", "полная занятость"), WordItem("internship", "стажировка")
        )),
        Lesson(id = "work_4", title = "Work 4", iconEmoji = "💼", words = listOf(
            WordItem("probation period", "испытательный срок"), WordItem("staff turnover", "текучесть кадров"), WordItem("human resources", "отдел кадров"), WordItem("performance review", "оценка работы"),
            WordItem("job description", "описание должности"), WordItem("responsibilities", "обязанности"), WordItem("multitasking", "многозадачность"), WordItem("productivity", "производительность"),
            WordItem("workplace stress", "стресс на работе"), WordItem("labour market", "рынок труда")
        )),
        Lesson(id = "work_5", title = "Work 5", iconEmoji = "💼", words = listOf(
            WordItem("job interview", "собеседование"), WordItem("occupational hazard", "профессиональный риск"), WordItem("employment contract", "трудовой договор"), WordItem("freelancer", "фрилансер"),
            WordItem("self-employed", "самозанятый"), WordItem("entrepreneurship", "предпринимательство"), WordItem("job stability", "стабильность работы"), WordItem("networking", "нетворкинг"),
            WordItem("career advancement", "карьерный рост"), WordItem("professional", "профессионал")
        ))
    )
    
    // TOPIC 7: RELATIONSHIPS
    private val relationshipsLessons = listOf(
        Lesson(id = "relationships_1", title = "Relationships 1", iconEmoji = "❤️", words = listOf(
            WordItem("mutual understanding", "взаимопонимание"), WordItem("commitment", "преданность"), WordItem("trust issues", "проблемы с доверием"), WordItem("emotional support", "эмоциональная поддержка"),
            WordItem("companionship", "товарищество"), WordItem("conflict", "конфликт"), WordItem("argument", "спор"), WordItem("reconciliation", "примирение"),
            WordItem("loyalty", "верность"), WordItem("affection", "привязанность")
        )),
        Lesson(id = "relationships_2", title = "Relationships 2", iconEmoji = "❤️", words = listOf(
            WordItem("empathy", "сочувствие"), WordItem("misunderstanding", "недоразумение"), WordItem("compromise", "компромисс"), WordItem("compatibility", "совместимость"),
            WordItem("long-term relationship", "долгосрочные отношения"), WordItem("marriage proposal", "предложение руки"), WordItem("separation", "разлука"), WordItem("divorce rate", "уровень разводов"),
            WordItem("domestic responsibilities", "домашние обязанности"), WordItem("generation gap", "разрыв поколений")
        )),
        Lesson(id = "relationships_3", title = "Relationships 3", iconEmoji = "❤️", words = listOf(
            WordItem("peer pressure", "давление сверстников"), WordItem("friendship circle", "круг друзей"), WordItem("emotional bond", "эмоциональная связь"), WordItem("trustworthiness", "надёжность"),
            WordItem("respect", "уважение"), WordItem("jealousy", "ревность"), WordItem("stability", "стабильность"), WordItem("reliability", "надёжность"),
            WordItem("emotional intelligence", "эмоциональный интеллект"), WordItem("communication skills", "навыки общения")
        )),
        Lesson(id = "relationships_4", title = "Relationships 4", iconEmoji = "❤️", words = listOf(
            WordItem("supportive partner", "поддерживающий партнёр"), WordItem("shared interests", "общие интересы"), WordItem("toxic relationship", "токсичные отношения"), WordItem("upbringing", "воспитание"),
            WordItem("parenting style", "стиль воспитания"), WordItem("sibling rivalry", "соперничество братьев/сестёр"), WordItem("extended family", "расширенная семья"), WordItem("nuclear family", "нуклеарная семья"),
            WordItem("close-knit family", "дружная семья"), WordItem("conflict resolution", "разрешение конфликтов")
        )),
        Lesson(id = "relationships_5", title = "Relationships 5", iconEmoji = "❤️", words = listOf(
            WordItem("emotional maturity", "эмоциональная зрелость"), WordItem("family values", "семейные ценности"), WordItem("intergenerational conflict", "межпоколенческий конфликт"), WordItem("relationship breakdown", "разрыв отношений"),
            WordItem("trust building", "построение доверия"), WordItem("bonding", "сближение"), WordItem("personal boundaries", "личные границы"), WordItem("social interaction", "социальное взаимодействие"),
            WordItem("understanding", "понимание"), WordItem("caring", "забота")
        ))
    )
    
    // TOPIC 8: TOWN & CITY
    private val townCityLessons = listOf(
        Lesson(id = "towncity_1", title = "Town & City 1", iconEmoji = "🏙", words = listOf(
            WordItem("urbanisation", "урбанизация"), WordItem("infrastructure", "инфраструктура"), WordItem("public transport", "общественный транспорт"), WordItem("traffic congestion", "пробки"),
            WordItem("overcrowding", "перенаселённость"), WordItem("air pollution", "загрязнение воздуха"), WordItem("noise pollution", "шумовое загрязнение"), WordItem("green spaces", "зелёные зоны"),
            WordItem("residential area", "жилой район"), WordItem("commercial district", "торговый район")
        )),
        Lesson(id = "towncity_2", title = "Town & City 2", iconEmoji = "🏙", words = listOf(
            WordItem("city centre", "центр города"), WordItem("suburban area", "пригородная зона"), WordItem("housing shortage", "нехватка жилья"), WordItem("public amenities", "общественные удобства"),
            WordItem("pedestrian zone", "пешеходная зона"), WordItem("rush hour", "час пик"), WordItem("population density", "плотность населения"), WordItem("urban sprawl", "разрастание города"),
            WordItem("high-rise buildings", "высотные здания"), WordItem("industrial zone", "промышленная зона")
        )),
        Lesson(id = "towncity_3", title = "Town & City 3", iconEmoji = "🏙", words = listOf(
            WordItem("sustainable development", "устойчивое развитие"), WordItem("local authorities", "местные власти"), WordItem("public services", "государственные услуги"), WordItem("crime rate", "уровень преступности"),
            WordItem("quality of life", "качество жизни"), WordItem("public facilities", "общественные объекты"), WordItem("waste management", "управление отходами"), WordItem("city planning", "градостроительство"),
            WordItem("affordable housing", "доступное жильё"), WordItem("gentrification", "джентрификация")
        )),
        Lesson(id = "towncity_4", title = "Town & City 4", iconEmoji = "🏙", words = listOf(
            WordItem("urban regeneration", "обновление города"), WordItem("cultural diversity", "культурное разнообразие"), WordItem("economic growth", "экономический рост"), WordItem("transport network", "транспортная сеть"),
            WordItem("road infrastructure", "дорожная инфраструктура"), WordItem("city council", "городской совет"), WordItem("municipal services", "муниципальные услуги"), WordItem("environmental sustainability", "экологическая устойчивость"),
            WordItem("nightlife", "ночная жизнь"), WordItem("heritage site", "объект наследия")
        )),
        Lesson(id = "towncity_5", title = "Town & City 5", iconEmoji = "🏙", words = listOf(
            WordItem("tourism industry", "туристическая индустрия"), WordItem("public safety", "общественная безопасность"), WordItem("urban lifestyle", "городской образ жизни"), WordItem("commuter", "пригородный житель"),
            WordItem("relocation", "переезд"), WordItem("business district", "деловой район"), WordItem("rural area", "сельская местность"), WordItem("capital city", "столица"),
            WordItem("metropolitan", "мегаполис"), WordItem("downtown", "центр города")
        ))
    )
    
    // TOPIC 9: BOOKS & FILMS
    private val booksFilmsLessons = listOf(
        Lesson(id = "booksfilms_1", title = "Books & Films 1", iconEmoji = "🎬", words = listOf(
            WordItem("plot development", "развитие сюжета"), WordItem("storyline", "сюжетная линия"), WordItem("protagonist", "главный герой"), WordItem("antagonist", "антагонист"),
            WordItem("character development", "развитие персонажа"), WordItem("narrative structure", "структура повествования"), WordItem("climax", "кульминация"), WordItem("twist ending", "неожиданная концовка"),
            WordItem("setting", "место действия"), WordItem("genre", "жанр")
        )),
        Lesson(id = "booksfilms_2", title = "Books & Films 2", iconEmoji = "🎬", words = listOf(
            WordItem("science fiction", "научная фантастика"), WordItem("documentary", "документальный фильм"), WordItem("adaptation", "экранизация"), WordItem("screenplay", "сценарий"),
            WordItem("cinematography", "кинематография"), WordItem("soundtrack", "саундтрек"), WordItem("box office", "кассовые сборы"), WordItem("special effects", "спецэффекты"),
            WordItem("critically acclaimed", "признанный критиками"), WordItem("best-selling novel", "бестселлер")
        )),
        Lesson(id = "booksfilms_3", title = "Books & Films 3", iconEmoji = "🎬", words = listOf(
            WordItem("fiction", "художественная литература"), WordItem("non-fiction", "нон-фикшн"), WordItem("autobiography", "автобиография"), WordItem("biography", "биография"),
            WordItem("sequel", "продолжение"), WordItem("trilogy", "трилогия"), WordItem("director", "режиссёр"), WordItem("producer", "продюсер"),
            WordItem("scriptwriter", "сценарист"), WordItem("plot twist", "поворот сюжета")
        )),
        Lesson(id = "booksfilms_4", title = "Books & Films 4", iconEmoji = "🎬", words = listOf(
            WordItem("emotional impact", "эмоциональное воздействие"), WordItem("suspense", "напряжение"), WordItem("dramatic tension", "драматическое напряжение"), WordItem("moral message", "нравственный посыл"),
            WordItem("literary device", "литературный приём"), WordItem("symbolism", "символизм"), WordItem("metaphor", "метафора"), WordItem("award-winning", "отмеченный наградами"),
            WordItem("audience engagement", "вовлечение аудитории"), WordItem("publishing industry", "издательская индустрия")
        )),
        Lesson(id = "booksfilms_5", title = "Books & Films 5", iconEmoji = "🎬", words = listOf(
            WordItem("bestseller list", "список бестселлеров"), WordItem("film industry", "киноиндустрия"), WordItem("character arc", "арка персонажа"), WordItem("narrative technique", "повествовательная техника"),
            WordItem("film adaptation", "экранизация"), WordItem("historical drama", "историческая драма"), WordItem("fantasy genre", "жанр фэнтези"), WordItem("thriller", "триллер"),
            WordItem("romantic comedy", "романтическая комедия"), WordItem("blockbuster", "блокбастер")
        ))
    )
    
    // TOPIC 10: CLOTHES & FASHION
    private val clothesFashionLessons = listOf(
        Lesson(id = "fashion_1", title = "Fashion 1", iconEmoji = "👗", words = listOf(
            WordItem("fashion industry", "индустрия моды"), WordItem("trendsetter", "законодатель моды"), WordItem("designer brand", "дизайнерский бренд"), WordItem("sustainable fashion", "устойчивая мода"),
            WordItem("fast fashion", "быстрая мода"), WordItem("second-hand clothing", "подержанная одежда"), WordItem("dress code", "дресс-код"), WordItem("formal attire", "формальная одежда"),
            WordItem("casual wear", "повседневная одежда"), WordItem("vintage clothing", "винтажная одежда")
        )),
        Lesson(id = "fashion_2", title = "Fashion 2", iconEmoji = "👗", words = listOf(
            WordItem("accessories", "аксессуары"), WordItem("footwear", "обувь"), WordItem("fabric", "ткань"), WordItem("cotton", "хлопок"),
            WordItem("silk", "шёлк"), WordItem("leather", "кожа"), WordItem("synthetic materials", "синтетические материалы"), WordItem("fashionable", "модный"),
            WordItem("outdated", "устаревший"), WordItem("stylish", "стильный")
        )),
        Lesson(id = "fashion_3", title = "Fashion 3", iconEmoji = "👗", words = listOf(
            WordItem("elegant", "элегантный"), WordItem("smart-casual", "смарт-кэжуал"), WordItem("brand-conscious", "бренд-ориентированный"), WordItem("fashion statement", "модное заявление"),
            WordItem("wardrobe", "гардероб"), WordItem("outfit", "наряд"), WordItem("tailor-made", "сшитый на заказ"), WordItem("designer label", "дизайнерская марка"),
            WordItem("fashion show", "показ мод"), WordItem("runway", "подиум")
        )),
        Lesson(id = "fashion_4", title = "Fashion 4", iconEmoji = "👗", words = listOf(
            WordItem("seasonal collection", "сезонная коллекция"), WordItem("affordable fashion", "доступная мода"), WordItem("clothing line", "линия одежды"), WordItem("luxury brand", "люксовый бренд"),
            WordItem("consumer trends", "потребительские тренды"), WordItem("ethical production", "этичное производство"), WordItem("mass production", "массовое производство"), WordItem("textile industry", "текстильная промышленность"),
            WordItem("fashion retailer", "модный ритейлер"), WordItem("marketing strategy", "маркетинговая стратегия")
        )),
        Lesson(id = "fashion_5", title = "Fashion 5", iconEmoji = "👗", words = listOf(
            WordItem("image-conscious", "заботящийся об имидже"), WordItem("appearance", "внешний вид"), WordItem("trend", "тренд"), WordItem("fashionable item", "модная вещь"),
            WordItem("clothing store", "магазин одежды"), WordItem("outfit choice", "выбор наряда"), WordItem("personal style", "личный стиль"), WordItem("colour coordination", "сочетание цветов"),
            WordItem("dressing appropriately", "уместная одежда"), WordItem("fashion sense", "чувство моды")
        ))
    )
    
    // TOPIC 11: MUSIC
    private val musicLessons = listOf(
        Lesson(id = "music_1", title = "Music 1", iconEmoji = "🎵", words = listOf(
            WordItem("genre", "жанр"), WordItem("melody", "мелодия"), WordItem("rhythm", "ритм"), WordItem("harmony", "гармония"),
            WordItem("lyrics", "текст песни"), WordItem("live performance", "живое выступление"), WordItem("concert venue", "концертная площадка"), WordItem("festival", "фестиваль"),
            WordItem("band", "группа"), WordItem("orchestra", "оркестр")
        )),
        Lesson(id = "music_2", title = "Music 2", iconEmoji = "🎵", words = listOf(
            WordItem("solo artist", "сольный исполнитель"), WordItem("classical music", "классическая музыка"), WordItem("pop music", "поп-музыка"), WordItem("hip-hop", "хип-хоп"),
            WordItem("rock band", "рок-группа"), WordItem("album release", "выход альбома"), WordItem("music industry", "музыкальная индустрия"), WordItem("streaming service", "стриминговый сервис"),
            WordItem("digital download", "цифровая загрузка"), WordItem("musical instrument", "музыкальный инструмент")
        )),
        Lesson(id = "music_3", title = "Music 3", iconEmoji = "🎵", words = listOf(
            WordItem("composer", "композитор"), WordItem("songwriter", "автор песен"), WordItem("recording studio", "студия звукозаписи"), WordItem("sound quality", "качество звука"),
            WordItem("background music", "фоновая музыка"), WordItem("soundtrack", "саундтрек"), WordItem("chart-topping", "возглавляющий чарты"), WordItem("commercial success", "коммерческий успех"),
            WordItem("audience engagement", "вовлечение аудитории"), WordItem("fan base", "фанатская база")
        )),
        Lesson(id = "music_4", title = "Music 4", iconEmoji = "🎵", words = listOf(
            WordItem("rehearsal", "репетиция"), WordItem("musical talent", "музыкальный талант"), WordItem("vocal ability", "вокальные способности"), WordItem("music education", "музыкальное образование"),
            WordItem("cultural influence", "культурное влияние"), WordItem("emotional impact", "эмоциональное воздействие"), WordItem("live concert", "живой концерт"), WordItem("ticket sales", "продажа билетов"),
            WordItem("performance skills", "навыки выступления"), WordItem("musical career", "музыкальная карьера")
        )),
        Lesson(id = "music_5", title = "Music 5", iconEmoji = "🎵", words = listOf(
            WordItem("traditional music", "традиционная музыка"), WordItem("contemporary music", "современная музыка"), WordItem("artistic expression", "художественное выражение"), WordItem("music preference", "музыкальные предпочтения"),
            WordItem("entertainment industry", "индустрия развлечений"), WordItem("music festival", "музыкальный фестиваль"), WordItem("public performance", "публичное выступление"), WordItem("musical event", "музыкальное событие"),
            WordItem("acoustic", "акустический"), WordItem("amplifier", "усилитель")
        ))
    )
    
    // ========== ADVANCED LEVEL ==========
    
    // TOPIC 12: TECHNOLOGY
    private val technologyLessons = listOf(
        Lesson(id = "technology_1", title = "Technology 1", iconEmoji = "💻", words = listOf(
            WordItem("technological advancement", "технологический прогресс"), WordItem("artificial intelligence", "искусственный интеллект"), WordItem("automation", "автоматизация"), WordItem("cybersecurity", "кибербезопасность"),
            WordItem("data privacy", "конфиденциальность данных"), WordItem("digital transformation", "цифровая трансформация"), WordItem("innovation", "инновация"), WordItem("high-tech devices", "высокотехнологичные устройства"),
            WordItem("cutting-edge technology", "передовая технология"), WordItem("software development", "разработка ПО")
        )),
        Lesson(id = "technology_2", title = "Technology 2", iconEmoji = "💻", words = listOf(
            WordItem("hardware components", "аппаратные компоненты"), WordItem("cloud computing", "облачные вычисления"), WordItem("big data", "большие данные"), WordItem("algorithm", "алгоритм"),
            WordItem("machine learning", "машинное обучение"), WordItem("virtual reality", "виртуальная реальность"), WordItem("augmented reality", "дополненная реальность"), WordItem("data breach", "утечка данных"),
            WordItem("encryption", "шифрование"), WordItem("network security", "сетевая безопасность")
        )),
        Lesson(id = "technology_3", title = "Technology 3", iconEmoji = "💻", words = listOf(
            WordItem("online platform", "онлайн-платформа"), WordItem("digital literacy", "цифровая грамотность"), WordItem("social media addiction", "зависимость от соцсетей"), WordItem("cyberbullying", "кибербуллинг"),
            WordItem("technological dependence", "зависимость от технологий"), WordItem("information overload", "информационная перегрузка"), WordItem("remote communication", "удалённое общение"), WordItem("teleconferencing", "телеконференция"),
            WordItem("digital footprint", "цифровой след"), WordItem("user interface", "пользовательский интерфейс")
        )),
        Lesson(id = "technology_4", title = "Technology 4", iconEmoji = "💻", words = listOf(
            WordItem("online privacy", "конфиденциальность в сети"), WordItem("internet access", "доступ в интернет"), WordItem("broadband connection", "широкополосное подключение"), WordItem("digital divide", "цифровое неравенство"),
            WordItem("e-commerce", "электронная коммерция"), WordItem("robotics", "робототехника"), WordItem("automation process", "процесс автоматизации"), WordItem("technical support", "техническая поддержка"),
            WordItem("data storage", "хранение данных"), WordItem("information technology", "информационные технологии")
        )),
        Lesson(id = "technology_5", title = "Technology 5", iconEmoji = "💻", words = listOf(
            WordItem("digital revolution", "цифровая революция"), WordItem("screen time", "экранное время"), WordItem("wearable devices", "носимые устройства"), WordItem("smart devices", "умные устройства"),
            WordItem("online security", "онлайн-безопасность"), WordItem("software update", "обновление ПО"), WordItem("cybercrime", "киберпреступность"), WordItem("technological breakthrough", "технологический прорыв"),
            WordItem("programming", "программирование"), WordItem("database", "база данных")
        ))
    )
    
    // TOPIC 13: SPORTS
    private val sportsLessons = listOf(
        Lesson(id = "sports_1", title = "Sports 1", iconEmoji = "⚽️", words = listOf(
            WordItem("physical endurance", "физическая выносливость"), WordItem("stamina", "выносливость"), WordItem("competitive spirit", "соревновательный дух"), WordItem("teamwork", "командная работа"),
            WordItem("referee", "судья"), WordItem("championship", "чемпионат"), WordItem("tournament", "турнир"), WordItem("athletic performance", "спортивные результаты"),
            WordItem("training regime", "тренировочный режим"), WordItem("professional athlete", "профессиональный спортсмен")
        )),
        Lesson(id = "sports_2", title = "Sports 2", iconEmoji = "⚽️", words = listOf(
            WordItem("amateur level", "любительский уровень"), WordItem("physical fitness", "физическая форма"), WordItem("injury prevention", "профилактика травм"), WordItem("rehabilitation", "реабилитация"),
            WordItem("sportsmanship", "спортивное поведение"), WordItem("spectator sport", "зрелищный спорт"), WordItem("coaching staff", "тренерский штаб"), WordItem("physical strength", "физическая сила"),
            WordItem("coordination", "координация"), WordItem("tactics", "тактика")
        )),
        Lesson(id = "sports_3", title = "Sports 3", iconEmoji = "⚽️", words = listOf(
            WordItem("strategy", "стратегия"), WordItem("performance enhancement", "улучшение результатов"), WordItem("doping scandal", "допинговый скандал"), WordItem("sports facilities", "спортивные сооружения"),
            WordItem("international competition", "международное соревнование"), WordItem("qualification round", "квалификационный раунд"), WordItem("knockout stage", "стадия плей-офф"), WordItem("physical activity", "физическая активность"),
            WordItem("cardiovascular health", "здоровье сердца"), WordItem("fitness industry", "фитнес-индустрия")
        )),
        Lesson(id = "sports_4", title = "Sports 4", iconEmoji = "⚽️", words = listOf(
            WordItem("team captain", "капитан команды"), WordItem("substitute player", "запасной игрок"), WordItem("league table", "турнирная таблица"), WordItem("sporting event", "спортивное событие"),
            WordItem("Olympic Games", "Олимпийские игры"), WordItem("world championship", "чемпионат мира"), WordItem("sponsorship deal", "спонсорский контракт"), WordItem("media coverage", "освещение в СМИ"),
            WordItem("training session", "тренировка"), WordItem("athletic ability", "спортивные способности")
        )),
        Lesson(id = "sports_5", title = "Sports 5", iconEmoji = "⚽️", words = listOf(
            WordItem("sports equipment", "спортивное оборудование"), WordItem("fan support", "поддержка фанатов"), WordItem("physical training", "физическая подготовка"), WordItem("grassroots sport", "массовый спорт"),
            WordItem("talent development", "развитие таланта"), WordItem("fitness routine", "фитнес-программа"), WordItem("sports career", "спортивная карьера"), WordItem("injury recovery", "восстановление после травмы"),
            WordItem("competition", "соревнование"), WordItem("victory", "победа")
        ))
    )
    
    // TOPIC 14: HEALTH
    private val healthLessons = listOf(
        Lesson(id = "health_1", title = "Health 1", iconEmoji = "🧠", words = listOf(
            WordItem("mental health", "психическое здоровье"), WordItem("physical wellbeing", "физическое благополучие"), WordItem("chronic disease", "хроническое заболевание"), WordItem("infectious disease", "инфекционное заболевание"),
            WordItem("immune system", "иммунная система"), WordItem("medical treatment", "медицинское лечение"), WordItem("preventive care", "профилактика"), WordItem("healthcare system", "система здравоохранения"),
            WordItem("health insurance", "медицинская страховка"), WordItem("life expectancy", "продолжительность жизни")
        )),
        Lesson(id = "health_2", title = "Health 2", iconEmoji = "🧠", words = listOf(
            WordItem("medical research", "медицинские исследования"), WordItem("public health", "общественное здоровье"), WordItem("vaccination programme", "программа вакцинации"), WordItem("side effects", "побочные эффекты"),
            WordItem("prescription medication", "рецептурные лекарства"), WordItem("balanced lifestyle", "сбалансированный образ жизни"), WordItem("sedentary lifestyle", "сидячий образ жизни"), WordItem("obesity epidemic", "эпидемия ожирения"),
            WordItem("stress management", "управление стрессом"), WordItem("mental disorder", "психическое расстройство")
        )),
        Lesson(id = "health_3", title = "Health 3", iconEmoji = "🧠", words = listOf(
            WordItem("psychological support", "психологическая поддержка"), WordItem("diagnosis", "диагноз"), WordItem("symptoms", "симптомы"), WordItem("recovery process", "процесс выздоровления"),
            WordItem("surgical procedure", "хирургическая операция"), WordItem("primary care", "первичная помощь"), WordItem("specialist", "специалист"), WordItem("medical staff", "медицинский персонал"),
            WordItem("healthcare access", "доступ к медицине"), WordItem("rehabilitation", "реабилитация")
        )),
        Lesson(id = "health_4", title = "Health 4", iconEmoji = "🧠", words = listOf(
            WordItem("therapy session", "сеанс терапии"), WordItem("addiction", "зависимость"), WordItem("substance abuse", "злоупотребление веществами"), WordItem("healthcare funding", "финансирование здравоохранения"),
            WordItem("hospital admission", "госпитализация"), WordItem("health awareness", "осведомлённость о здоровье"), WordItem("preventive measures", "профилактические меры"), WordItem("genetic disorder", "генетическое заболевание"),
            WordItem("outbreak", "вспышка заболевания"), WordItem("pandemic", "пандемия")
        )),
        Lesson(id = "health_5", title = "Health 5", iconEmoji = "🧠", words = listOf(
            WordItem("health campaign", "кампания по здоровью"), WordItem("wellbeing", "благополучие"), WordItem("health services", "медицинские услуги"), WordItem("healthcare professionals", "медицинские работники"),
            WordItem("nutrition", "питание"), WordItem("life-threatening condition", "угрожающее жизни состояние"), WordItem("public awareness", "общественная осведомлённость"), WordItem("mental resilience", "психическая устойчивость"),
            WordItem("fitness", "физическая форма"), WordItem("wellness", "оздоровление")
        ))
    )
    
    // TOPIC 15: BUSINESS
    private val businessLessons = listOf(
        Lesson(id = "business_1", title = "Business 1", iconEmoji = "💼", words = listOf(
            WordItem("entrepreneurship", "предпринимательство"), WordItem("start-up", "стартап"), WordItem("investment", "инвестиция"), WordItem("shareholder", "акционер"),
            WordItem("profit margin", "маржа прибыли"), WordItem("revenue", "доход"), WordItem("expenditure", "расходы"), WordItem("financial stability", "финансовая стабильность"),
            WordItem("economic growth", "экономический рост"), WordItem("supply and demand", "спрос и предложение")
        )),
        Lesson(id = "business_2", title = "Business 2", iconEmoji = "💼", words = listOf(
            WordItem("market competition", "рыночная конкуренция"), WordItem("consumer demand", "потребительский спрос"), WordItem("multinational corporation", "транснациональная корпорация"), WordItem("business strategy", "бизнес-стратегия"),
            WordItem("marketing campaign", "маркетинговая кампания"), WordItem("brand recognition", "узнаваемость бренда"), WordItem("customer satisfaction", "удовлетворённость клиента"), WordItem("innovation", "инновация"),
            WordItem("productivity", "производительность"), WordItem("workforce", "рабочая сила")
        )),
        Lesson(id = "business_3", title = "Business 3", iconEmoji = "💼", words = listOf(
            WordItem("management structure", "структура управления"), WordItem("corporate responsibility", "корпоративная ответственность"), WordItem("ethical practices", "этичные практики"), WordItem("stakeholder", "заинтересованная сторона"),
            WordItem("partnership", "партнёрство"), WordItem("merger", "слияние"), WordItem("acquisition", "поглощение"), WordItem("economic downturn", "экономический спад"),
            WordItem("inflation rate", "уровень инфляции"), WordItem("unemployment rate", "уровень безработицы")
        )),
        Lesson(id = "business_4", title = "Business 4", iconEmoji = "💼", words = listOf(
            WordItem("financial crisis", "финансовый кризис"), WordItem("global market", "мировой рынок"), WordItem("competitive advantage", "конкурентное преимущество"), WordItem("business expansion", "расширение бизнеса"),
            WordItem("trade agreement", "торговое соглашение"), WordItem("export", "экспорт"), WordItem("import", "импорт"), WordItem("economic policy", "экономическая политика"),
            WordItem("tax revenue", "налоговые поступления"), WordItem("government subsidy", "государственная субсидия")
        )),
        Lesson(id = "business_5", title = "Business 5", iconEmoji = "💼", words = listOf(
            WordItem("market research", "исследование рынка"), WordItem("product development", "разработка продукта"), WordItem("sales revenue", "выручка от продаж"), WordItem("return on investment", "возврат инвестиций"),
            WordItem("business model", "бизнес-модель"), WordItem("corporate culture", "корпоративная культура"), WordItem("leadership", "лидерство"), WordItem("decision-making", "принятие решений"),
            WordItem("operational costs", "операционные расходы"), WordItem("profit", "прибыль")
        ))
    )
    
    // TOPIC 16: ENVIRONMENT
    private val environmentLessons = listOf(
        Lesson(id = "environment_1", title = "Environment 1", iconEmoji = "🌍", words = listOf(
            WordItem("climate change", "изменение климата"), WordItem("global warming", "глобальное потепление"), WordItem("carbon footprint", "углеродный след"), WordItem("renewable energy", "возобновляемая энергия"),
            WordItem("fossil fuels", "ископаемое топливо"), WordItem("greenhouse gases", "парниковые газы"), WordItem("deforestation", "вырубка лесов"), WordItem("biodiversity", "биоразнообразие"),
            WordItem("conservation", "охрана природы"), WordItem("natural resources", "природные ресурсы")
        )),
        Lesson(id = "environment_2", title = "Environment 2", iconEmoji = "🌍", words = listOf(
            WordItem("environmental protection", "защита окружающей среды"), WordItem("sustainability", "устойчивость"), WordItem("pollution levels", "уровень загрязнения"), WordItem("waste disposal", "утилизация отходов"),
            WordItem("recycling programme", "программа переработки"), WordItem("environmental awareness", "экологическая осведомлённость"), WordItem("ecosystem", "экосистема"), WordItem("endangered species", "вымирающие виды"),
            WordItem("habitat destruction", "разрушение среды обитания"), WordItem("water scarcity", "дефицит воды")
        )),
        Lesson(id = "environment_3", title = "Environment 3", iconEmoji = "🌍", words = listOf(
            WordItem("air contamination", "загрязнение воздуха"), WordItem("environmental impact", "воздействие на среду"), WordItem("overpopulation", "перенаселение"), WordItem("urban expansion", "расширение городов"),
            WordItem("environmental policy", "экологическая политика"), WordItem("climate crisis", "климатический кризис"), WordItem("ecological balance", "экологический баланс"), WordItem("sustainable development", "устойчивое развитие"),
            WordItem("alternative energy", "альтернативная энергия"), WordItem("environmental damage", "экологический ущерб")
        )),
        Lesson(id = "environment_4", title = "Environment 4", iconEmoji = "🌍", words = listOf(
            WordItem("conservation efforts", "усилия по охране"), WordItem("organic farming", "органическое земледелие"), WordItem("carbon emissions", "выбросы углерода"), WordItem("plastic waste", "пластиковые отходы"),
            WordItem("industrial waste", "промышленные отходы"), WordItem("environmental legislation", "экологическое законодательство"), WordItem("green energy", "зелёная энергия"), WordItem("renewable resources", "возобновляемые ресурсы"),
            WordItem("natural disaster", "стихийное бедствие"), WordItem("rising sea levels", "повышение уровня моря")
        )),
        Lesson(id = "environment_5", title = "Environment 5", iconEmoji = "🌍", words = listOf(
            WordItem("environmental campaign", "экологическая кампания"), WordItem("energy efficiency", "энергоэффективность"), WordItem("environmental responsibility", "экологическая ответственность"), WordItem("climate action", "климатические действия"),
            WordItem("sustainable lifestyle", "устойчивый образ жизни"), WordItem("water pollution", "загрязнение воды"), WordItem("soil erosion", "эрозия почвы"), WordItem("environmental sustainability", "экологическая устойчивость"),
            WordItem("ecology", "экология"), WordItem("nature", "природа")
        ))
    )
    
    // TOPIC 17: ADVERTISING
    private val advertisingLessons = listOf(
        Lesson(id = "advertising_1", title = "Advertising 1", iconEmoji = "📢", words = listOf(
            WordItem("advertising campaign", "рекламная кампания"), WordItem("target audience", "целевая аудитория"), WordItem("brand awareness", "узнаваемость бренда"), WordItem("consumer behaviour", "поведение потребителя"),
            WordItem("persuasive techniques", "методы убеждения"), WordItem("marketing strategy", "маркетинговая стратегия"), WordItem("product placement", "продакт-плейсмент"), WordItem("sponsorship", "спонсорство"),
            WordItem("promotional material", "рекламные материалы"), WordItem("mass media", "средства массовой информации")
        )),
        Lesson(id = "advertising_2", title = "Advertising 2", iconEmoji = "📢", words = listOf(
            WordItem("social media marketing", "маркетинг в соцсетях"), WordItem("billboard", "рекламный щит"), WordItem("commercial break", "рекламная пауза"), WordItem("endorsement", "одобрение"),
            WordItem("celebrity endorsement", "реклама знаменитостью"), WordItem("advertising budget", "рекламный бюджет"), WordItem("brand image", "имидж бренда"), WordItem("slogan", "слоган"),
            WordItem("catchphrase", "крылатая фраза"), WordItem("market research", "исследование рынка")
        )),
        Lesson(id = "advertising_3", title = "Advertising 3", iconEmoji = "📢", words = listOf(
            WordItem("customer engagement", "вовлечение клиентов"), WordItem("digital marketing", "цифровой маркетинг"), WordItem("online advertising", "онлайн-реклама"), WordItem("advertising industry", "рекламная индустрия"),
            WordItem("competition", "конкуренция"), WordItem("public relations", "связи с общественностью"), WordItem("word-of-mouth", "сарафанное радио"), WordItem("brand loyalty", "лояльность к бренду"),
            WordItem("product launch", "запуск продукта"), WordItem("visual appeal", "визуальная привлекательность")
        )),
        Lesson(id = "advertising_4", title = "Advertising 4", iconEmoji = "📢", words = listOf(
            WordItem("advertising ethics", "этика рекламы"), WordItem("misleading advertising", "вводящая в заблуждение реклама"), WordItem("consumer trust", "доверие потребителя"), WordItem("marketing tactics", "маркетинговые тактики"),
            WordItem("media coverage", "освещение в СМИ"), WordItem("audience reach", "охват аудитории"), WordItem("advertising revenue", "рекламный доход"), WordItem("commercial success", "коммерческий успех"),
            WordItem("viral marketing", "вирусный маркетинг"), WordItem("influencer marketing", "маркетинг влияния")
        )),
        Lesson(id = "advertising_5", title = "Advertising 5", iconEmoji = "📢", words = listOf(
            WordItem("promotional offer", "рекламное предложение"), WordItem("discount campaign", "кампания скидок"), WordItem("brand recognition", "узнаваемость бренда"), WordItem("advertising agency", "рекламное агентство"),
            WordItem("customer base", "клиентская база"), WordItem("market share", "доля рынка"), WordItem("direct marketing", "прямой маркетинг"), WordItem("advertising regulations", "правила рекламы"),
            WordItem("promotion", "продвижение"), WordItem("publicity", "публичность")
        ))
    )
    
    // TOPIC 18: PERSONALITY
    private val personalityLessons = listOf(
        Lesson(id = "personality_1", title = "Personality 1", iconEmoji = "👤", words = listOf(
            WordItem("ambitious", "амбициозный"), WordItem("determined", "решительный"), WordItem("resilient", "стойкий"), WordItem("reliable", "надёжный"),
            WordItem("trustworthy", "заслуживающий доверия"), WordItem("compassionate", "сострадательный"), WordItem("empathetic", "эмпатичный"), WordItem("optimistic", "оптимистичный"),
            WordItem("pessimistic", "пессимистичный"), WordItem("introverted", "замкнутый")
        )),
        Lesson(id = "personality_2", title = "Personality 2", iconEmoji = "👤", words = listOf(
            WordItem("extroverted", "общительный"), WordItem("self-confident", "уверенный в себе"), WordItem("insecure", "неуверенный"), WordItem("responsible", "ответственный"),
            WordItem("disciplined", "дисциплинированный"), WordItem("open-minded", "открытый"), WordItem("narrow-minded", "ограниченный"), WordItem("stubborn", "упрямый"),
            WordItem("flexible", "гибкий"), WordItem("adaptable", "приспосабливаемый")
        )),
        Lesson(id = "personality_3", title = "Personality 3", iconEmoji = "👤", words = listOf(
            WordItem("creative", "творческий"), WordItem("innovative", "новаторский"), WordItem("independent", "независимый"), WordItem("dependent", "зависимый"),
            WordItem("mature", "зрелый"), WordItem("immature", "незрелый"), WordItem("generous", "щедрый"), WordItem("selfish", "эгоистичный"),
            WordItem("patient", "терпеливый"), WordItem("impatient", "нетерпеливый")
        )),
        Lesson(id = "personality_4", title = "Personality 4", iconEmoji = "👤", words = listOf(
            WordItem("considerate", "внимательный"), WordItem("insensitive", "бесчувственный"), WordItem("supportive", "поддерживающий"), WordItem("competitive", "конкурентный"),
            WordItem("cooperative", "сотрудничающий"), WordItem("charismatic", "харизматичный"), WordItem("modest", "скромный"), WordItem("arrogant", "высокомерный"),
            WordItem("honest", "честный"), WordItem("sincere", "искренний")
        )),
        Lesson(id = "personality_5", title = "Personality 5", iconEmoji = "👤", words = listOf(
            WordItem("loyal", "преданный"), WordItem("sociable", "общительный"), WordItem("reserved", "сдержанный"), WordItem("decisive", "решительный"),
            WordItem("indecisive", "нерешительный"), WordItem("hardworking", "трудолюбивый"), WordItem("lazy", "ленивый"), WordItem("sensitive", "чувствительный"),
            WordItem("emotionally stable", "эмоционально стабильный"), WordItem("confident", "уверенный")
        ))
    )
    
    // TOPIC 19: PHYSICAL APPEARANCE
    private val physicalAppearanceLessons = listOf(
        Lesson(id = "appearance_1", title = "Appearance 1", iconEmoji = "👁", words = listOf(
            WordItem("well-built", "крепкого телосложения"), WordItem("slim", "стройный"), WordItem("overweight", "с лишним весом"), WordItem("muscular", "мускулистый"),
            WordItem("athletic", "атлетичный"), WordItem("pale", "бледный"), WordItem("tanned", "загорелый"), WordItem("complexion", "цвет лица"),
            WordItem("facial features", "черты лица"), WordItem("wrinkles", "морщины")
        )),
        Lesson(id = "appearance_2", title = "Appearance 2", iconEmoji = "👁", words = listOf(
            WordItem("freckles", "веснушки"), WordItem("beard", "борода"), WordItem("moustache", "усы"), WordItem("hairstyle", "причёска"),
            WordItem("shoulder-length hair", "волосы до плеч"), WordItem("curly hair", "кудрявые волосы"), WordItem("straight hair", "прямые волосы"), WordItem("bald", "лысый"),
            WordItem("medium height", "среднего роста"), WordItem("tall", "высокий")
        )),
        Lesson(id = "appearance_3", title = "Appearance 3", iconEmoji = "👁", words = listOf(
            WordItem("short", "низкий"), WordItem("attractive", "привлекательный"), WordItem("plain-looking", "невзрачный"), WordItem("elegant", "элегантный"),
            WordItem("stylish", "стильный"), WordItem("neatly dressed", "аккуратно одетый"), WordItem("scruffy", "неопрятный"), WordItem("casual appearance", "повседневный вид"),
            WordItem("formal appearance", "формальный вид"), WordItem("distinctive features", "отличительные черты")
        )),
        Lesson(id = "appearance_4", title = "Appearance 4", iconEmoji = "👁", words = listOf(
            WordItem("bright eyes", "яркие глаза"), WordItem("dark circles", "тёмные круги"), WordItem("slim figure", "стройная фигура"), WordItem("well-dressed", "хорошо одетый"),
            WordItem("fashionable", "модный"), WordItem("trendy", "трендовый"), WordItem("charming smile", "обаятельная улыбка"), WordItem("confident posture", "уверенная осанка"),
            WordItem("physical traits", "физические черты"), WordItem("appearance-conscious", "следящий за внешностью")
        )),
        Lesson(id = "appearance_5", title = "Appearance 5", iconEmoji = "👁", words = listOf(
            WordItem("well-groomed", "ухоженный"), WordItem("messy hair", "растрёпанные волосы"), WordItem("youthful appearance", "молодой вид"), WordItem("aged look", "возрастной вид"),
            WordItem("average build", "среднее телосложение"), WordItem("striking appearance", "яркая внешность"), WordItem("facial expression", "выражение лица"), WordItem("slim build", "худощавое телосложение"),
            WordItem("fit", "подтянутый"), WordItem("handsome", "красивый")
        ))
    )
    
    // ========== ALL UNITS ==========
    val allUnits = listOf(
        // BEGINNER LEVEL (5 topics)
        LearningUnit(id = "education", title = "Education", description = "Academic vocabulary for IELTS", lessons = educationLessons, iconEmoji = "📚", color = 0xFF4CAF50, requiredXP = 0),
        LearningUnit(id = "accommodation", title = "Accommodation", description = "Housing and living vocabulary", lessons = accommodationLessons, iconEmoji = "🏠", color = 0xFF2196F3, requiredXP = 100),
        LearningUnit(id = "food", title = "Food", description = "Food and nutrition vocabulary", lessons = foodLessons, iconEmoji = "🍽", color = 0xFFFF9800, requiredXP = 200),
        LearningUnit(id = "shopping", title = "Shopping", description = "Consumer and retail vocabulary", lessons = shoppingLessons, iconEmoji = "🛍", color = 0xFFE91E63, requiredXP = 300),
        LearningUnit(id = "weather", title = "Weather", description = "Climate and weather vocabulary", lessons = weatherLessons, iconEmoji = "🌤", color = 0xFF00BCD4, requiredXP = 400),
        
        // UPPER-INTERMEDIATE LEVEL (6 topics)
        LearningUnit(id = "work", title = "Work", description = "Employment and career vocabulary", lessons = workLessons, iconEmoji = "💼", color = 0xFF795548, requiredXP = 500),
        LearningUnit(id = "relationships", title = "Relationships", description = "Social and family vocabulary", lessons = relationshipsLessons, iconEmoji = "❤️", color = 0xFFF44336, requiredXP = 600),
        LearningUnit(id = "town_city", title = "Town & City", description = "Urban life vocabulary", lessons = townCityLessons, iconEmoji = "🏙", color = 0xFF607D8B, requiredXP = 700),
        LearningUnit(id = "books_films", title = "Books & Films", description = "Entertainment vocabulary", lessons = booksFilmsLessons, iconEmoji = "🎬", color = 0xFF9C27B0, requiredXP = 800),
        LearningUnit(id = "clothes_fashion", title = "Clothes & Fashion", description = "Fashion vocabulary", lessons = clothesFashionLessons, iconEmoji = "👗", color = 0xFFFF4081, requiredXP = 900),
        LearningUnit(id = "music", title = "Music", description = "Music industry vocabulary", lessons = musicLessons, iconEmoji = "🎵", color = 0xFF673AB7, requiredXP = 1000),
        
        // ADVANCED LEVEL (8 topics)
        LearningUnit(id = "technology", title = "Technology", description = "Tech and digital vocabulary", lessons = technologyLessons, iconEmoji = "💻", color = 0xFF3F51B5, requiredXP = 1100),
        LearningUnit(id = "sports", title = "Sports", description = "Sports and fitness vocabulary", lessons = sportsLessons, iconEmoji = "⚽️", color = 0xFF8BC34A, requiredXP = 1200),
        LearningUnit(id = "health", title = "Health", description = "Medical and health vocabulary", lessons = healthLessons, iconEmoji = "🧠", color = 0xFF009688, requiredXP = 1300),
        LearningUnit(id = "business", title = "Business", description = "Business and economics vocabulary", lessons = businessLessons, iconEmoji = "💼", color = 0xFF455A64, requiredXP = 1400),
        LearningUnit(id = "environment", title = "Environment", description = "Environmental vocabulary", lessons = environmentLessons, iconEmoji = "🌍", color = 0xFF4CAF50, requiredXP = 1500),
        LearningUnit(id = "advertising", title = "Advertising", description = "Marketing vocabulary", lessons = advertisingLessons, iconEmoji = "📢", color = 0xFFFF5722, requiredXP = 1600),
        LearningUnit(id = "personality", title = "Personality", description = "Character traits vocabulary", lessons = personalityLessons, iconEmoji = "👤", color = 0xFF9E9E9E, requiredXP = 1700),
        LearningUnit(id = "physical_appearance", title = "Physical Appearance", description = "Appearance vocabulary", lessons = physicalAppearanceLessons, iconEmoji = "👁", color = 0xFFCDDC39, requiredXP = 1800)
    )
}

object LearningPathManager {
    private const val PREFS = "learning_path"
    
    private const val KEY_CURRENT_UNIT = "current_unit"
    private const val KEY_CURRENT_LESSON = "current_lesson"
    private const val KEY_LESSON_PREFIX = "lesson_"
    private const val KEY_UNIT_PREFIX = "unit_"
    
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    
    /**
     * Get all units with their progress
     */
    fun getUnitsWithProgress(ctx: Context): List<Pair<LearningUnit, LearningUnitProgress>> {
        val p = prefs(ctx)
        val totalXP = StreakManager.getStreakState(ctx).totalXP
        
        return LearningPathData.allUnits.mapIndexed { index, unit ->
            val lessonsCompleted = unit.lessons.count { lesson ->
                getLessonProgress(ctx, lesson.id).crowns > 0
            }
            
            val isUnlocked = when {
                index == 0 -> true // First unit always unlocked
                totalXP >= unit.requiredXP -> true
                else -> {
                    // Check if previous unit is completed
                    val prevUnit = LearningPathData.allUnits.getOrNull(index - 1)
                    prevUnit?.let { prev ->
                        prev.lessons.all { lesson ->
                            getLessonProgress(ctx, lesson.id).crowns >= 1
                        }
                    } ?: false
                }
            }
            
            val unitTestPassed = p.getBoolean("${KEY_UNIT_PREFIX}${unit.id}_test_passed", false)
            
            unit to LearningUnitProgress(
                unitId = unit.id,
                lessonsCompleted = lessonsCompleted,
                totalLessons = unit.lessons.size,
                unitTestPassed = unitTestPassed,
                isUnlocked = isUnlocked
            )
        }
    }
    
    /**
     * Get progress for a specific lesson
     */
    fun getLessonProgress(ctx: Context, lessonId: String): LessonProgress {
        val p = prefs(ctx)
        val prefix = "${KEY_LESSON_PREFIX}${lessonId}_"
        
        return LessonProgress(
            lessonId = lessonId,
            crowns = p.getInt("${prefix}crowns", 0),
            wordsLearned = p.getInt("${prefix}words_learned", 0),
            totalWords = p.getInt("${prefix}total_words", 0),
            lastPracticed = p.getLong("${prefix}last_practiced", 0),
            strength = p.getFloat("${prefix}strength", 1f),
            perfectCompletions = p.getInt("${prefix}perfect_completions", 0)
        )
    }
    
    /**
     * Complete a lesson and update progress
     * Note: Lesson completion is NOT dependent on correct answers count.
     * After completing a lesson, the next lesson unlocks automatically.
     * Errors are tracked only for statistics and learning, not for progression.
     */
    fun completeLesson(
        ctx: Context,
        lessonId: String,
        wordsCorrect: Int,
        totalWords: Int,
        isPerfect: Boolean
    ): LessonCompletionResult {
        val p = prefs(ctx)
        val prefix = "${KEY_LESSON_PREFIX}${lessonId}_"
        
        val currentCrowns = p.getInt("${prefix}crowns", 0)
        val perfectCompletions = p.getInt("${prefix}perfect_completions", 0)
        
        // Calculate new crowns - always at least 1 crown for completing the lesson
        val newPerfectCompletions = if (isPerfect) perfectCompletions + 1 else perfectCompletions
        val newCrowns = when {
            newPerfectCompletions >= 5 -> 5 // Legendary
            newPerfectCompletions >= 4 -> 4 // Platinum
            newPerfectCompletions >= 3 -> 3 // Gold
            newPerfectCompletions >= 2 -> 2 // Silver
            else -> 1 // Always at least Bronze for completing the lesson
        }.coerceAtLeast(currentCrowns)
        
        val crownLevel = CrownLevel.entries.find { it.level == newCrowns } ?: CrownLevel.BRONZE
        // Base XP for completing lesson + bonus for correct answers
        val xpEarned = 10 + (5 * wordsCorrect) + crownLevel.xpBonus
        
        // Save progress
        p.edit()
            .putInt("${prefix}crowns", newCrowns)
            .putInt("${prefix}words_learned", wordsCorrect)
            .putInt("${prefix}total_words", totalWords)
            .putLong("${prefix}last_practiced", System.currentTimeMillis())
            .putFloat("${prefix}strength", 1f)
            .putInt("${prefix}perfect_completions", newPerfectCompletions)
            .apply()
        
        // Add XP
        StreakManager.addXP(ctx, xpEarned)
        
        val earnedNewCrown = newCrowns > currentCrowns
        
        return LessonCompletionResult(
            xpEarned = xpEarned,
            newCrowns = newCrowns,
            earnedNewCrown = earnedNewCrown,
            isPerfect = isPerfect,
            crownLevel = crownLevel
        )
    }
    
    /**
     * Get next lesson to practice (weakest or next uncompleted)
     */
    fun getNextLesson(ctx: Context): Pair<LearningUnit, Lesson>? {
        val unitsWithProgress = getUnitsWithProgress(ctx)
        
        for ((unit, progress) in unitsWithProgress) {
            if (!progress.isUnlocked) continue
            
            // Find first lesson without a crown
            for (lesson in unit.lessons) {
                val lessonProgress = getLessonProgress(ctx, lesson.id)
                if (lessonProgress.crowns == 0) {
                    return unit to lesson
                }
            }
        }
        
        // All lessons have at least one crown - find weakest
        var weakestLesson: Pair<LearningUnit, Lesson>? = null
        var weakestStrength = 2f
        
        for ((unit, progress) in unitsWithProgress) {
            if (!progress.isUnlocked) continue
            
            for (lesson in unit.lessons) {
                val lessonProgress = getLessonProgress(ctx, lesson.id)
                if (lessonProgress.strength < weakestStrength && lessonProgress.crowns < 5) {
                    weakestStrength = lessonProgress.strength
                    weakestLesson = unit to lesson
                }
            }
        }
        
        return weakestLesson
    }
    
    /**
     * Get lessons that need practice (strength < 0.5)
     */
    fun getLessonsNeedingPractice(ctx: Context): List<Pair<LearningUnit, Lesson>> {
        val result = mutableListOf<Pair<LearningUnit, Lesson>>()
        
        for (unit in LearningPathData.allUnits) {
            for (lesson in unit.lessons) {
                val progress = getLessonProgress(ctx, lesson.id)
                if (progress.crowns > 0 && progress.strength < 0.5f) {
                    result.add(unit to lesson)
                }
            }
        }
        
        return result.sortedBy { getLessonProgress(ctx, it.second.id).strength }
    }
    
    /**
     * Decay lesson strength over time (call daily)
     */
    fun decayLessonStrength(ctx: Context) {
        val p = prefs(ctx)
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        
        for (unit in LearningPathData.allUnits) {
            for (lesson in unit.lessons) {
                val prefix = "${KEY_LESSON_PREFIX}${lesson.id}_"
                val lastPracticed = p.getLong("${prefix}last_practiced", 0)
                val currentStrength = p.getFloat("${prefix}strength", 1f)
                val crowns = p.getInt("${prefix}crowns", 0)
                
                if (crowns == 0 || lastPracticed == 0L) continue
                
                val daysSinceLastPractice = (now - lastPracticed) / dayMs
                if (daysSinceLastPractice > 0) {
                    // Decay rate depends on crown level (higher crowns = slower decay)
                    val decayRate = 0.1f / crowns
                    val newStrength = (currentStrength - decayRate * daysSinceLastPractice)
                        .coerceIn(0f, 1f)
                    
                    p.edit().putFloat("${prefix}strength", newStrength).apply()
                }
            }
        }
    }
    
    /**
     * Get total progress stats
     */
    fun getTotalStats(ctx: Context): LearningPathStats {
        var totalLessons = 0
        var completedLessons = 0
        var totalCrowns = 0
        var maxCrowns = 0
        var totalWords = 0
        var learnedWords = 0
        
        for (unit in LearningPathData.allUnits) {
            for (lesson in unit.lessons) {
                totalLessons++
                maxCrowns += 5
                totalWords += lesson.words.size
                
                val progress = getLessonProgress(ctx, lesson.id)
                if (progress.crowns > 0) {
                    completedLessons++
                    totalCrowns += progress.crowns
                    learnedWords += progress.wordsLearned
                }
            }
        }
        
        return LearningPathStats(
            totalLessons = totalLessons,
            completedLessons = completedLessons,
            totalCrowns = totalCrowns,
            maxCrowns = maxCrowns,
            totalWords = totalWords,
            learnedWords = learnedWords
        )
    }
}

data class LessonCompletionResult(
    val xpEarned: Int,
    val newCrowns: Int,
    val earnedNewCrown: Boolean,
    val isPerfect: Boolean,
    val crownLevel: CrownLevel
)

data class LearningPathStats(
    val totalLessons: Int,
    val completedLessons: Int,
    val totalCrowns: Int,
    val maxCrowns: Int,
    val totalWords: Int,
    val learnedWords: Int
) {
    val completionPercent: Float
        get() = if (totalLessons > 0) completedLessons.toFloat() / totalLessons else 0f
    
    val crownPercent: Float
        get() = if (maxCrowns > 0) totalCrowns.toFloat() / maxCrowns else 0f
}
