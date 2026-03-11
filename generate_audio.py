"""
ElevenLabs Audio Generator for GecTyping
=========================================
Генерирует MP3 файлы для всех слов в игре.

Два голоса:
- Jessica (Jessie) - для Learning Path (темы IELTS)
- Adam - для Spelling Game (уровни)

Использование:
python generate_audio.py

После генерации скопируй файлы в app/src/main/res/raw/
"""

import os
import sys
import time
import requests
import re

# API ключ ElevenLabs
API_KEY = "sk_85d9660d0604febc6d608f44b4e03dc82b8c0dfa538c7c1e"

# ElevenLabs API
ELEVENLABS_API_URL = "https://api.elevenlabs.io/v1/text-to-speech"

# Голоса
VOICE_JESSIE = "t0jbNlBVZ17f02VDIeMI"  # Jessie - женский голос для Learning Path
VOICE_ADAM = "pNInz6obpgDQGcFmaJgB"    # Adam - мужской голос для Spelling Game

# ============ SPELLING GAME WORDS (7 уровней) - голос Adam ============
SPELLING_WORDS = """
cash, debit, credit card, cheque, advance, fee, monthly, membership, interest, deposit, fees, poverty, bank, statement, money, management, current, account, student, withdraw, low, risk, investment, mortgage, grace, period, budget, voucher, coupon, public, taxpayers, debt, credit, purchase, refund, annuity, non, refundable, card, distribution, costs, income, finance, department, family, finances, duty, free, store,
science, politics, history, biology, architecture, law, geography, literature, business, agriculture, statistics, mathematics, logic, physics, psychology, economics, philosophy, arts, chemistry, humanities, course, outline, group, discussion, handout, written, work, report, research, proofreading, experiment, experience, reference, textbook, dictionary, laptop, printer, advisor, teamwork, module, topic, assessment, library, computer, centre, classroom, lecture, tutor, hall, attendance, deadline, talk, speech, laboratory, certificate, diploma, test, students, full, time, facilities, college, dining, room, specialist, knowledge, international, accommodation, home, stay, primary, secondary, intermediate, media, resources, staff, commencement, dissertation, leaflet, faculty, pupils, pencil, feedback, tasks, outcomes, advanced, introductory, background, higher, education, guidelines, post, supervisor, bachelor, degree, compound, vocabulary, support, services, retention, publication, foreign, schedule, school, reunion, registrar, office, stationery,
catalogue, interview, newsletter, competition, program, strategies, method, entertainment, industry, leadership, display, products, customer, special, offer, collecting, data, questionnaire, survey, mass, statistic, profit, margin, poll, training, trainee, merchandise, manufacture, recruitment, yoga, keep, fit, salad, bar, vegetarian, outdoor, activities, leisure, disease, meal, protein, balanced, diet, food, pyramid, vitamin, carbohydrates, rice, pasta, potatoes, pizza, tomatoes, bread, cereals, minerals, zinc, meat, seafood, eggs, beans, milk, cheese, yoghurt, fruit, vegetables, citrus, fruits, green, pepper, nuts, egg, yolk, liver, medicine, treatment, remedy, nursing, care, nursery, regular, exercise,
field, footbridge, environment, waterfall, river, mountain, forest, village, coast, reef, lake, valley, hill, cliff, island, peninsula, earthquake, avalanche, tornado, typhoon, desertification, volcano, disaster, catastrophe, erosion, landslide, storm, flood, hurricane, pond, jungle, oasis, dam, canyon, greenhouse, effect, acid, rain, global, warming, carbon, dioxide, burning, fossil, exhaust, fumes, deforestation, nitrogen, oxide, smog, climate, pollution, temperature, power, plants, landfill, cattle, wind, turbine, solar, hydroelectric, renewable, source, energy, reliable, panels, environmentally, friendly, oxygen, chemical, degradation, vegetation, sea, level, ocean, currents, soil, conditioner, coal, fuels, firewood, drought, contaminated,
birds, prey, seabirds, poultry, game, mammals, cetacean, whale, primates, rodents, fish, amphibian, reptile, insects, octopus, phylum, class, order, genus, species, livestock, creature, lion, penguin, mushroom, fungus, leaves, seed, core, bark, trunk, twig, branch, flower, stem, roots, cluster, fertilizer, south, america, north, africa, asia, europe, australia, antarctica, egypt, mexico, france, indonesia, turkey, england, germany, china, greece, brazil, india, korea, malaysia, zealand, nigeria, pakistan, singapore, switzerland, kingdom, italy, dominican, republic, philippines, denmark, linguistics, bilingual, trilingual, polyglot, portuguese, mandarin, bengali, chinese, hindi, russian, japanese, german, punjabi, thai, persian, filipino, french, italian, greek,
dome, palace, fort, castle, glasshouse, pyramid, log, cabin, lighthouse, hut, skyscraper, sculpture, semi, detached, house, duplex, terraced, town, row, bungalow, thatched, cottage, mobile, houseboat, block, flats, apartment, building, condominium, chimney, bedroom, basement, landlord, tenant, rent, lease, neighborhood, suburb, sofa, coffee, table, dormitory, storey, kitchen, refrigerator, microwave, ground, floor, oven, hallway, insurance, cities, street, lane, city, central, station, car, park, store, bridge, temple, embassy, road, system, hospital, garden, avenue, clinic, dentist, reception, appointment, selection, colleague, workshop, showroom, information, desk, employer, employment, unemployed, technical, cooperation, team, leaders, stress, ability, vision, confidence, employee, internship,
reasonable, satisfactory, dangerous, safe, strongly, recommended, poor, quality, satisfied, disappointed, efficient, luxurious, colored, spotted, striped, expensive, cheap, tourist, guided, tour, ticket, souvenir, trip, guest, reservation, view, culture, memorable, single, double, bedded, picnic, attraction, hostel, suite, aquarium, train, develop, collect, supervise, mark, edit, revise, exhibit, donate, surpass, register, hunt, persuade, concentrate, discuss, suggest, arrange, borrow, immigrate, review, learn, touch, energetic, social, ancient, necessary, fantastic, exciting, fabulous, dull, comfortable, convenient, suitable, affordable, voluntary, mandatory, compulsory, temporary, permanent, immense, vast, salty, extinct, vulnerable, pessimistic, optimistic, realistic, practical, knowledgeable, flexible, confident, western, intensive, tranquil, spectacular, intact, various, orienteering, caving, spelunking, archery, skating, diving, snorkeling, skateboarding, bowls, darts, golf, billiards, photography, painting, pottery, woodcarving, gardening, stamp, collection, embroidery, climbing, chess, parachute
""".strip()

# ============ LEARNING PATH WORDS (темы IELTS) - голос Jessie ============
LEARNING_PATH_WORDS = """
curriculum, syllabus, assessment, coursework, assignment, dissertation, tuition fees, scholarship, grant, undergraduate, postgraduate, vocational training, distance learning, academic performance, literacy, numeracy, compulsory education, higher education, state school, private institution, extracurricular activities, enrolment, deadline, seminar, lecture hall, supervisor, research paper, citation, plagiarism, qualification, diploma, degree, foundation course, academic year, gap year, student loan, internship, peer review, academic achievement, discipline, faculty, tutorial, grading system, thesis statement, methodology, critical thinking, revision, workload, attendance, certification,
mortgage, tenancy, landlord, tenant, lease agreement, utilities, deposit, rent increase, furnished, unfurnished, maintenance, renovation, suburb, residential area, accommodation shortage, property market, estate agent, housing estate, detached house, semi-detached, terraced house, high-rise building, studio flat, shared accommodation, affordable housing, council housing, eviction, household bills, security system, soundproof, central heating, insulation, overcrowded, amenities, relocation, homeownership, accommodation crisis, living conditions, spacious, cramped, ventilation, balcony, basement, attic, property value, real estate, infrastructure, neighbourhood, relocation expenses, housing,
nutritional value, processed food, home-cooked meal, organic produce, artificial additives, preservatives, balanced diet, calorie intake, portion size, saturated fat, sugar intake, whole grains, plant-based diet, food poisoning, food safety, expiry date, takeaway meal, food chain, dietary restrictions, food labelling, obesity rate, malnutrition, metabolism, immune system, culinary skills, cooking methods, boiling, steaming, grilling, frying, seasoning, ingredients, staple food, fast food consumption, eating habits, food waste, agriculture, local produce, food industry, organic farming, genetically modified, supply chain, food shortage, food security, undernourished, appetite, digestion, contamination, nutrition, healthy eating,
consumer behaviour, retail outlet, online purchase, discount, refund policy, impulse buying, brand loyalty, counterfeit goods, warranty, receipt, bulk buying, window shopping, competitive pricing, shopping mall, customer service, bargain, luxury goods, expenditure, price comparison, overconsumption, retail therapy, second-hand goods, ethical shopping, product range, availability, exchange policy, delivery fee, consumer rights, market demand, sales promotion, advertising campaign, loyalty card, instalment plan, seasonal sale, clearance sale, supplier, wholesaler, retailer, pricing strategy, shopping habits, product quality, limited edition, stock shortage, purchasing power, return policy, online retailer, doorstep delivery, pre-order, promotional offer, customer satisfaction,
forecast, humidity, precipitation, drought, heatwave, cold spell, climate conditions, temperature drop, thunderstorm, lightning strike, snowfall, strong winds, overcast, clear skies, freezing point, mild weather, seasonal change, climate change, extreme weather, hurricane, rainfall, weather patterns, global warming, carbon emissions, greenhouse effect, rising temperatures, coastal flooding, natural disaster, weather warning, storm surge, drought-resistant, humid climate, arid region, unpredictable weather, sunny intervals, chilly breeze, tropical climate, climate zone, heavy downpour, air pressure, seasonal variation, weather conditions, environmental impact, heat exhaustion, icy roads, foggy conditions, scorching heat, temperature,
employment rate, unemployment, job security, career prospects, promotion, redundancy, workload, deadline pressure, work-life balance, job satisfaction, salary raise, benefits package, pension scheme, recruitment process, job vacancy, qualifications, professional development, training programme, career ladder, managerial position, leadership skills, teamwork, conflict resolution, workplace environment, corporate culture, flexible hours, remote work, part-time employment, full-time position, probation period, staff turnover, human resources, performance review, job description, responsibilities, multitasking, productivity, workplace stress, labour market, job interview, occupational hazard, employment contract, freelancer, self-employed, entrepreneurship, job stability, networking, career advancement, professional,
mutual understanding, commitment, trust issues, emotional support, companionship, conflict, argument, reconciliation, loyalty, affection, empathy, misunderstanding, compromise, compatibility, long-term relationship, marriage proposal, separation, divorce rate, domestic responsibilities, generation gap, peer pressure, friendship circle, emotional bond, trustworthiness, respect, jealousy, stability, reliability, emotional intelligence, communication skills, supportive partner, shared interests, toxic relationship, upbringing, parenting style, sibling rivalry, extended family, nuclear family, close-knit family, emotional maturity, family values, intergenerational conflict, relationship breakdown, trust building, bonding, personal boundaries, social interaction, understanding, caring,
urbanisation, public transport, traffic congestion, overcrowding, air pollution, noise pollution, green spaces, commercial district, city centre, suburban area, housing shortage, public amenities, pedestrian zone, rush hour, population density, urban sprawl, high-rise buildings, industrial zone, sustainable development, local authorities, public services, crime rate, quality of life, public facilities, waste management, city planning, gentrification, urban regeneration, cultural diversity, economic growth, transport network, road infrastructure, city council, municipal services, environmental sustainability, nightlife, heritage site, tourism industry, public safety, urban lifestyle, commuter, business district, rural area, capital city, metropolitan, downtown,
plot development, storyline, protagonist, antagonist, character development, narrative structure, climax, twist ending, setting, genre, science fiction, documentary, adaptation, screenplay, cinematography, soundtrack, box office, special effects, critically acclaimed, best-selling novel, fiction, non-fiction, autobiography, biography, sequel, trilogy, director, producer, scriptwriter, plot twist, emotional impact, suspense, dramatic tension, moral message, literary device, symbolism, metaphor, award-winning, audience engagement, publishing industry, bestseller list, film industry, character arc, narrative technique, film adaptation, historical drama, fantasy genre, thriller, romantic comedy, blockbuster,
fashion industry, trendsetter, designer brand, sustainable fashion, fast fashion, second-hand clothing, dress code, formal attire, casual wear, vintage clothing, accessories, footwear, fabric, cotton, silk, leather, synthetic materials, fashionable, outdated, stylish, elegant, smart-casual, brand-conscious, fashion statement, wardrobe, outfit, tailor-made, designer label, fashion show, runway, seasonal collection, affordable fashion, clothing line, luxury brand, consumer trends, ethical production, mass production, textile industry, fashion retailer, marketing strategy, image-conscious, appearance, trend, fashionable item, clothing store, outfit choice, personal style, colour coordination, dressing appropriately, fashion sense,
melody, rhythm, harmony, lyrics, live performance, concert venue, festival, band, orchestra, solo artist, classical music, pop music, hip-hop, rock band, album release, music industry, streaming service, digital download, musical instrument, composer, songwriter, recording studio, sound quality, background music, chart-topping, commercial success, fan base, rehearsal, musical talent, vocal ability, music education, cultural influence, live concert, ticket sales, performance skills, musical career, traditional music, contemporary music, artistic expression, music preference, entertainment industry, music festival, public performance, musical event, acoustic, amplifier,
technological advancement, artificial intelligence, automation, cybersecurity, data privacy, digital transformation, innovation, high-tech devices, cutting-edge technology, software development, hardware components, cloud computing, big data, algorithm, machine learning, virtual reality, augmented reality, data breach, encryption, network security, online platform, digital literacy, social media addiction, cyberbullying, technological dependence, information overload, remote communication, teleconferencing, digital footprint, user interface, online privacy, internet access, broadband connection, digital divide, e-commerce, robotics, automation process, technical support, data storage, information technology, digital revolution, screen time, wearable devices, smart devices, online security, software update, cybercrime, technological breakthrough, programming, database,
physical endurance, stamina, competitive spirit, referee, championship, tournament, athletic performance, training regime, professional athlete, amateur level, physical fitness, injury prevention, rehabilitation, sportsmanship, spectator sport, coaching staff, physical strength, coordination, tactics, strategy, performance enhancement, doping scandal, sports facilities, international competition, qualification round, knockout stage, physical activity, cardiovascular health, fitness industry, team captain, substitute player, league table, sporting event, Olympic Games, world championship, sponsorship deal, media coverage, training session, athletic ability, sports equipment, fan support, physical training, grassroots sport, talent development, fitness routine, sports career, injury recovery, competition, victory,
mental health, physical wellbeing, chronic disease, infectious disease, medical treatment, preventive care, healthcare system, health insurance, life expectancy, medical research, public health, vaccination programme, side effects, prescription medication, balanced lifestyle, sedentary lifestyle, obesity epidemic, stress management, mental disorder, psychological support, diagnosis, symptoms, recovery process, surgical procedure, primary care, specialist, medical staff, healthcare access, therapy session, addiction, substance abuse, healthcare funding, hospital admission, health awareness, preventive measures, genetic disorder, outbreak, pandemic, health campaign, wellbeing, health services, healthcare professionals, life-threatening condition, public awareness, mental resilience, fitness, wellness,
start-up, investment, shareholder, profit margin, revenue, financial stability, supply and demand, market competition, consumer demand, multinational corporation, business strategy, marketing campaign, brand recognition, workforce, management structure, corporate responsibility, ethical practices, stakeholder, partnership, merger, acquisition, economic downturn, inflation rate, unemployment rate, financial crisis, global market, competitive advantage, business expansion, trade agreement, export, import, economic policy, tax revenue, government subsidy, market research, product development, sales revenue, return on investment, business model, leadership, decision-making, operational costs, profit,
carbon footprint, renewable energy, fossil fuels, greenhouse gases, biodiversity, conservation, natural resources, environmental protection, sustainability, pollution levels, waste disposal, recycling programme, environmental awareness, ecosystem, endangered species, habitat destruction, water scarcity, air contamination, overpopulation, urban expansion, environmental policy, climate crisis, ecological balance, alternative energy, environmental damage, conservation efforts, plastic waste, industrial waste, environmental legislation, green energy, renewable resources, rising sea levels, environmental campaign, energy efficiency, environmental responsibility, climate action, sustainable lifestyle, water pollution, soil erosion, ecology, nature,
target audience, brand awareness, persuasive techniques, product placement, sponsorship, promotional material, social media marketing, billboard, commercial break, endorsement, celebrity endorsement, advertising budget, brand image, slogan, catchphrase, customer engagement, digital marketing, online advertising, advertising industry, public relations, word-of-mouth, product launch, visual appeal, advertising ethics, misleading advertising, consumer trust, marketing tactics, audience reach, advertising revenue, viral marketing, influencer marketing, discount campaign, advertising agency, customer base, market share, direct marketing, advertising regulations, publicity,
ambitious, determined, resilient, reliable, trustworthy, compassionate, empathetic, optimistic, pessimistic, introverted, extroverted, self-confident, insecure, responsible, disciplined, open-minded, narrow-minded, stubborn, flexible, adaptable, creative, innovative, independent, dependent, mature, immature, generous, selfish, patient, impatient, considerate, insensitive, supportive, competitive, cooperative, charismatic, modest, arrogant, honest, sincere, loyal, sociable, reserved, decisive, indecisive, hardworking, lazy, sensitive, emotionally stable,
well-built, slim, overweight, muscular, athletic, pale, tanned, complexion, facial features, wrinkles, freckles, beard, moustache, hairstyle, shoulder-length hair, curly hair, straight hair, bald, medium height, tall, short, attractive, plain-looking, neatly dressed, scruffy, casual appearance, formal appearance, distinctive features, bright eyes, dark circles, slim figure, well-dressed, trendy, charming smile, confident posture, youthful appearance, aged appearance, grooming, physical appearance
""".strip()

def parse_words(text):
    """Извлекает уникальные слова из текста"""
    words = set()
    for word in text.replace('\n', ',').split(','):
        word = word.strip().lower()
        if word and len(word) > 1:
            words.add(word)
    return sorted(words)

def sanitize_filename(word, prefix="word"):
    """Преобразует слово в безопасное имя файла для Android res/raw"""
    safe = re.sub(r'[^a-z0-9]', '_', word.lower())
    safe = re.sub(r'_+', '_', safe).strip('_')
    return f"{prefix}_{safe}"

def generate_audio(word, voice_id, output_dir, prefix="word"):
    """Генерирует аудио для одного слова"""
    filename = sanitize_filename(word, prefix)
    output_path = os.path.join(output_dir, f"{filename}.mp3")
    
    # Пропускаем если файл уже существует
    if os.path.exists(output_path):
        print(f"  [SKIP] {word}")
        return True, True  # success, skipped
    
    headers = {
        "Accept": "audio/mpeg",
        "Content-Type": "application/json",
        "xi-api-key": API_KEY
    }
    
    data = {
        "text": word,
        "model_id": "eleven_monolingual_v1",
        "voice_settings": {
            "stability": 0.5,
            "similarity_boost": 0.75
        }
    }
    
    try:
        response = requests.post(
            f"{ELEVENLABS_API_URL}/{voice_id}",
            json=data,
            headers=headers
        )
        
        if response.status_code == 200:
            with open(output_path, 'wb') as f:
                f.write(response.content)
            print(f"  [OK] {word}")
            return True, False
        else:
            error_msg = response.text[:100] if response.text else "Unknown error"
            print(f"  [ERROR] {word}: {response.status_code} - {error_msg}")
            return False, False
    except Exception as e:
        print(f"  [ERROR] {word}: {e}")
        return False, False

def generate_batch(words, voice_id, voice_name, output_dir, prefix="word"):
    """Генерирует аудио для списка слов"""
    print(f"\n{'='*50}")
    print(f"Голос: {voice_name}")
    print(f"Слов: {len(words)}")
    print(f"{'='*50}")
    
    success = 0
    skipped = 0
    failed = 0
    
    for i, word in enumerate(words, 1):
        print(f"[{i}/{len(words)}] {word}")
        ok, was_skipped = generate_audio(word, voice_id, output_dir, prefix)
        if ok:
            success += 1
            if was_skipped:
                skipped += 1
        else:
            failed += 1
        
        # Задержка чтобы не превысить лимит
        if not was_skipped:
            time.sleep(0.25)
    
    return success, skipped, failed

def main():
    output_dir = "audio_output"
    os.makedirs(output_dir, exist_ok=True)
    
    print("="*50)
    print("ElevenLabs Audio Generator for GecTyping")
    print("="*50)
    print(f"Выходная папка: {output_dir}/")
    
    # Парсим слова
    spelling_words = parse_words(SPELLING_WORDS)
    learning_words = parse_words(LEARNING_PATH_WORDS)
    
    # Убираем дубликаты между режимами (Learning Path приоритет)
    spelling_only = [w for w in spelling_words if w not in learning_words]
    
    print(f"\nSpelling Game (Adam): {len(spelling_only)} уникальных слов")
    print(f"Learning Path (Jessie): {len(learning_words)} слов")
    print(f"Всего: {len(spelling_only) + len(learning_words)} слов")
    
    # Генерируем для Spelling Game (Adam)
    s1, sk1, f1 = generate_batch(
        spelling_only, 
        VOICE_ADAM, 
        "Adam (Spelling Game)", 
        output_dir,
        "spell"  # prefix для Spelling
    )
    
    # Генерируем для Learning Path (Jessie)
    s2, sk2, f2 = generate_batch(
        learning_words, 
        VOICE_JESSIE, 
        "Jessie (Learning Path)", 
        output_dir,
        "learn"  # prefix для Learning Path
    )
    
    print("\n" + "="*50)
    print("ИТОГО:")
    print(f"  Успешно: {s1 + s2}")
    print(f"  Пропущено (уже есть): {sk1 + sk2}")
    print(f"  Ошибок: {f1 + f2}")
    print("="*50)
    print(f"\nСкопируй файлы из {output_dir}/ в:")
    print("app/src/main/res/raw/")

if __name__ == "__main__":
    main()
