/**
 *  SusiLanguage
 *  Copyright 15.07.2017 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package ai.susi.mind;

/**
 * ISO-639-1 language codes as enum
 */
public enum SusiLanguage {

    unknown("unknown", "unknown"), // this is a helper object; i.e. for a susi dream where the language is unknown
    aa("Afar", "Afaraf"),
    ab("Abkhaz", "аҧсуа бызшәа"),
    ae("Avestan", "avesta"),
    af("Afrikaans", "Afrikaans"),
    ak("Akan", "Akan"),
    am("Amharic", "አማርኛ"),
    an("Aragonese", "aragonés"),
    ar("Arabic", "اللغة العربية"),
    as("Assamese", "অসমীয়া"),
    av("Avaric", "авар мацӀ"),
    ay("Aymara", "aymar aru"),
    az("Azerbaijani", "azərbaycan dili"),
    ba("Bashkir", "башҡорт теле"),
    be("Belarusian", "беларуская мова"),
    bg("Bulgarian", "български език"),
    bh("Bihari", "भोजपुरी"),
    bi("Bislama", "Bislama"),
    bm("Bambara", "bamanankan"),
    bn("Bengali", "বাংলা"),
    bo("Tibetan Standard", "བོད་ཡིག"),
    br("Breton", "brezhoneg"),
    bs("Bosnian", "bosanski jezik"),
    ca("Catalan", "català"),
    ce("Chechen", "нохчийн мотт"),
    ch("Chamorro", "Chamoru"),
    co("Corsican", "corsu"),
    cr("Cree", "ᓀᐦᐃᔭᐍᐏᐣ"),
    cs("Czech", "čeština"),
    cu("Old Church Slavonic", "ѩзыкъ словѣньскъ"),
    cv("Chuvash", "чӑваш чӗлхи"),
    cy("Welsh", "Cymraeg"),
    da("Danish", "dansk"),
    de("German", "Deutsch"),
    dv("Divehi", "Dhivehi"),
    dz("Dzongkha", "རྫོང་ཁ"),
    ee("Ewe", "Eʋegbe"),
    el("Greek", "ελληνικά"),
    en("English", "English"),
    eo("Esperanto", "Esperanto"),
    es("Spanish", "Español"),
    et("Estonian", "eesti"),
    eu("Basque", "euskara"),
    fa("Persian", "فارسی"),
    ff("Fula", "Fulfulde"),
    fi("Finnish", "suomi"),
    fj("Fijian", "Vakaviti"),
    fo("Faroese", "føroyskt"),
    fr("French", "Français"),
    fy("Western Frisian", "Frysk"),
    ga("Irish", "Gaeilge"),
    gd("Scottish Gaelic", "Gàidhlig"),
    gl("Galician", "galego"),
    gn("Guaraní", "Avañe\'ẽ"),
    gu("Gujarati", "ગુજરાતી"),
    gv("Manx", "Gaelg"),
    ha("Hausa", "هَوُسَ"),
    he("Hebrew", "עברית"),
    hi("Hindi", "हिन्दी"),
    ho("Hiri Motu", "Hiri Motu"),
    hr("Croatian", "hrvatski jezik"),
    ht("Haitian", "Kreyòl ayisyen"),
    hu("Hungarian", "magyar"),
    hy("Armenian", "Հայերեն"),
    hz("Herero", "Otjiherero"),
    ia("Interlingua", "Interlingua"),
    id("Indonesian", "Indonesian"),
    ie("Interlingue", "Interlingue"),
    ig("Igbo", "Asụsụ Igbo"),
    ii("Nuosu", "ꆈꌠ꒿ Nuosuhxop"),
    ik("Inupiaq", "Iñupiaq"),
    io("Ido", "Ido"),
    is("Icelandic", "Íslenska"),
    it("Italian", "Italiano"),
    iu("Inuktitut", "ᐃᓄᒃᑎᑐᑦ"),
    ja("Japanese", "日本語"),
    jv("Javanese", "basa Jawa"),
    ka("Georgian", "ქართული"),
    kg("Kongo", "Kikongo"),
    ki("Kikuyu", "Gĩkũyũ"),
    kj("Kwanyama", "Kuanyama"),
    kk("Kazakh", "қазақ тілі"),
    kl("Kalaallisut", "kalaallisut"),
    km("Khmer", "ខេមរភាសា"),
    kn("Kannada", "ಕನ್ನಡ"),
    ko("Korean", "한국어"),
    kr("Kanuri", "Kanuri"),
    ks("Kashmiri", "कश्मीरी"),
    ku("Kurdish", "Kurdî"),
    kv("Komi", "коми кыв"),
    kw("Cornish", "Kernewek"),
    ky("Kyrgyz", "Кыргызча"),
    la("Latin", "latine"),
    lb("Luxembourgish", "Lëtzebuergesch"),
    lg("Ganda", "Luganda"),
    li("Limburgish", "Limburgs"),
    ln("Lingala", "Lingála"),
    lo("Lao", "ພາສາ"),
    lt("Lithuanian", "lietuvių kalba"),
    lu("Luba-Katanga", "Tshiluba"),
    lv("Latvian", "latviešu valoda"),
    mg("Malagasy", "fiteny malagasy"),
    mh("Marshallese", "Kajin M̧ajeļ"),
    mi("Māori", "te reo Māori"),
    mk("Macedonian", "македонски јазик"),
    ml("Malayalam", "മലയാളം"),
    mn("Mongolian", "Монгол хэл"),
    mr("Marathi", "मराठी"),
    ms("Malay", "هاس ملايو‎"),
    mt("Maltese", "Malti"),
    my("Burmese", "ဗမာစာ"),
    na("Nauru", "Ekakairũ Naoero"),
    nb("Norwegian Bokmål", "Norsk bokmål"),
    nd("Northern Ndebele", "isiNdebele"),
    ne("Nepali", "नेपाली"),
    ng("Ndonga", "Owambo"),
    nl("Dutch", "Nederlands"),
    nn("Norwegian Nynorsk", "Norsk nynorsk"),
    no("Norwegian", "Norsk"),
    nr("Southern Ndebele", "isiNdebele"),
    nv("Navajo", "Diné bizaad"),
    ny("Chichewa", "chiCheŵa"),
    oc("Occitan", "occitan"),
    oj("Ojibwe", "ᐊᓂᔑᓈᐯᒧᐎᓐ"),
    om("Oromo", "Afaan Oromoo"),
    or("Oriya", "ଓଡ଼ିଆ"),
    os("Ossetian", "ирон æвзаг"),
    pa("Panjabi", "ਪੰਜਾਬੀ"),
    pi("Pāli", "पाऴि"),
    pl("Polish", "język polski"),
    ps("Pashto", "پښتو"),
    pt("Portuguese", "Português"),
    qu("Quechua", "Runa Simi"),
    rm("Romansh", "rumantsch grischun"),
    rn("Kirundi", "Ikirundi"),
    ro("Romanian", "limba română"),
    ru("Russian", "Русский"),
    rw("Kinyarwanda", "Ikinyarwanda"),
    sa("Sanskrit", "संस्कृतम्"),
    sc("Sardinian", "sardu"),
    sd("Sindhi", "सिन्धी"),
    se("Northern Sami", "Davvisámegiella"),
    sg("Sango", "yângâ tî sängö"),
    si("Sinhala", "සිංහල"),
    sk("Slovak", "slovenčina"),
    sl("Slovene", "slovenski jezik"),
    sm("Samoan", "gagana fa\'a Samoa"),
    sn("Shona", "chiShona"),
    so("Somali", "Soomaaliga"),
    sq("Albanian", "Shqip"),
    sr("Serbian", "српски језик"),
    ss("Swati", "SiSwati"),
    st("Southern Sotho", "Sesotho"),
    su("Sundanese", "Basa Sunda"),
    sv("Swedish", "svenska"),
    sw("Swahili", "Kiswahili"),
    ta("Tamil", "தமிழ்"),
    te("Telugu", "తెలుగు"),
    tg("Tajik", "тоҷикӣ"),
    th("Thai", "ไทย"),
    ti("Tigrinya", "ትግርኛ"),
    tk("Turkmen", "Türkmen"),
    tl("Tagalog", "Wikang Tagalog"),
    tn("Tswana", "Setswana"),
    to("Tonga", "faka Tonga"),
    tr("Turkish", "Türkçe"),
    ts("Tsonga", "Xitsonga"),
    tt("Tatar", "татар теле"),
    tw("Twi", "Twi"),
    ty("Tahitian", "Reo Tahiti"),
    ug("Uyghur", "ئۇيغۇرچە‎"),
    uk("Ukrainian", "українська мова"),
    ur("Urdu", "اردو"),
    uz("Uzbek", "Ўзбек"),
    ve("Venda", "Tshivenḓa"),
    vi("Vietnamese", "Việt Nam"),
    vo("Volapük", "Volapük"),
    wa("Walloon", "walon"),
    wo("Wolof", "Wollof"),
    xh("Xhosa", "isiXhosa"),
    yi("Yiddish", "ייִדיש"),
    yo("Yoruba", "Yorùbá"),
    za("Zhuang", "Saɯ cueŋƅ"),
    zh("Chinese", "中文"),
    zu("Zulu", "isiZulu");
    
    private final String internationalName, nativeName;
    
    private SusiLanguage(String internationalName, String nativeName) {
        this.internationalName = internationalName;
        this.nativeName = nativeName;
    }
    
    public String getInternationalName() {
        return internationalName;
    }

    public String getNativeName() {
        return nativeName;
    }
    
    /**
     * Parse a string containing a language name.
     * This is convenience method which returns SusiLanguage.unknown in case that
     * the parsing fails.
     * @param language the string representatio of a ISO-639-1 language name
     * @return the language object or SusiLanguage.unknown if the language cannot be parsed
     */
    public static SusiLanguage parse(String language) {
        try {
            if (language.length() > 2) language = language.substring(0, 2);
            return SusiLanguage.valueOf(language.toLowerCase());
        } catch (IllegalArgumentException e) {
            return SusiLanguage.unknown;
        }
    }
    
    public float likelihoodCanSpeak(SusiLanguage other) {
        if (this == unknown) return 1.0f;
        if (this == other) return 1.0f;
        if ((this == de || this == fi || this == sv) && other == en) return 0.9f;
        if ((this == fi || this == sv) && (other == fi || other == sv)) return 0.9f;
        if (other == en) return 0.5f;
        return 0.0f;
    }
}
