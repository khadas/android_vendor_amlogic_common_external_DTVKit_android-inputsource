package com.droidlogic.dtvkit.inputsource;

import java.util.HashMap;
import java.util.Map;

public class ISO639Data {
    private static final Map<String, String> iso6392b2t_diff = new HashMap<>();
    private static final Map<String, String> iso3166_a3 = new HashMap<>();
    static {
        final String[][] entries = {
                //{ 3-letter in 2b,  3-letter in 2t},
                {"alb", "sqi"}, //Albanian
                {"arm", "hye"}, //Armenian
                {"baq", "eus"}, //Basque
                {"bur", "mya"}, //Burmese
                {"chi", "zho"}, //Chinese
                {"cze", "ces"}, //Czech
                {"dut", "nld"}, //Dutch
                {"fre", "fra"}, //French
                {"geo", "kat"}, //Georgian
                {"ger", "deu"}, //German
                {"gre", "ell"}, //Greek
                {"ice", "isl"}, //Icelandic
                {"mac", "mkd"}, //Macedonian
                {"may", "msa"}, //Malay
                {"mao", "mri"}, //Maori
                {"per", "fas"}, //Persian
                {"rum", "ron"}, //Romanian
                {"slo", "slk"}, //Slovak
                {"tib", "bod"}, //Tibetan
                {"wel", "cym"}, //Welsh
        };
        final String[][] entries_3166 = {
                //{3166,  country},
                {"abw", "Aruba"},
                {"afg", "Afghanistan"},
                {"ago", "Angola"},
                {"aia", "Anguilla"},
                {"ala", "Åland Islands"},
                {"alb", "Albania"},
                {"and", "Andorra"},
                {"are", "United Arab Emirates"},
                {"arg", "Argentina"},
                {"arm", "Armenia"},
                {"asm", "American Samoa"},
                {"ata", "Antarctica"},
                {"atf", "French Southern Territories"},
                {"atg", "Antigua and Barbuda"},
                {"aus", "Australia"},
                {"aut", "Austria"},
                {"aze", "Azerbaijan"},
                {"bdi", "Burundi"},
                {"bel", "Belgium"},
                {"ben", "Benin"},
                {"bes", "Bonaire, Sint Eustatius and Saba"},
                {"bfa", "Burkina Faso"},
                {"bgd", "Bangladesh"},
                {"bgr", "Bulgaria"},
                {"bhr", "Bahrain"},
                {"bhs", "Bahamas"},
                {"bih", "Bosnia and Herzegovina"},
                {"blm", "Saint Barthélemy"},
                {"blr", "Belarus"},
                {"blz", "Belize"},
                {"bmu", "Bermuda"},
                {"bol", "Bolivia"},
                {"bra", "Brazil"},
                {"brb", "Barbados"},
                {"brn", "Brunei Darussalam"},
                {"btn", "Bhutan"},
                {"bvt", "Bouvet Island"},
                {"bwa", "Botswana"},
                {"caf", "Central African Republic"},
                {"can", "Canada"},
                {"cck", "Cocos (Keeling) Islands"},
                {"che", "Switzerland"},
                {"chl", "Chile"},
                {"chn", "China"},
                {"civ", "Côte d'Ivoire"},
                {"cmr", "Cameroon"},
                {"cod", "Democratic Republic of the Congo"},
                {"cog", "Congo"},
                {"cok", "Cook Islands"},
                {"col", "Colombia"},
                {"com", "Comoros"},
                {"cpv", "Cabo Verde"},
                {"cri", "Costa Rica"},
                {"cub", "Cuba"},
                {"cuw", "Curaçao"},
                {"cxr", "Christmas Island"},
                {"cym", "Cayman Islands"},
                {"cyp", "Cyprus"},
                {"cze", "Czechia"},
                {"deu", "Germany"},
                {"dji", "Djibouti"},
                {"dma", "Dominica"},
                {"dnk", "Denmark"},
                {"dom", "Dominican Republic"},
                {"dza", "Algeria"},
                {"ecu", "Ecuador"},
                {"egy", "Egypt"},
                {"eri", "Eritrea"},
                {"esh", "Western Sahara"},
                {"esp", "Spain"},
                {"est", "Estonia"},
                {"eth", "Ethiopia"},
                {"fin", "Finland"},
                {"fji", "Fiji"},
                {"flk", "Falkland Islands"},
                {"fra", "France"},
                {"fro", "Faroe Islands"},
                {"fsm", "Federated States of Micronesia"},
                {"gab", "Gabon"},
                {"gbr", "United Kingdom"},
                {"geo", "Georgia"},
                {"ggy", "Guernsey"},
                {"gha", "Ghana"},
                {"gib", "Gibraltar"},
                {"gin", "Guinea"},
                {"glp", "Guadeloupe"},
                {"gmb", "Gambia"},
                {"gnb", "Guinea-Bissau"},
                {"gnq", "Equatorial Guinea"},
                {"grc", "Greece"},
                {"grd", "Grenada"},
                {"grl", "Greenland"},
                {"gtm", "Guatemala"},
                {"guf", "French Guiana"},
                {"gum", "Guam"},
                {"guy", "Guyana"},
                {"hkg", "Hong Kong(China)"},
                {"hmd", "Heard Island and McDonald Islands"},
                {"hnd", "Honduras"},
                {"hrv", "Croatia"},
                {"hti", "Haiti"},
                {"hun", "Hungary"},
                {"idn", "Indonesia"},
                {"imn", "Isle of Man"},
                {"ind", "India"},
                {"iot", "British Indian Ocean Territory"},
                {"irl", "Ireland"},
                {"irn", "Iran"},
                {"irq", "Iraq"},
                {"isl", "Iceland"},
                {"isr", "Israel"},
                {"ita", "Italy"},
                {"jam", "Jamaica"},
                {"jey", "Jersey"},
                {"jor", "Jordan"},
                {"jpn", "Japan"},
                {"kaz", "Kazakhstan"},
                {"ken", "Kenya"},
                {"kgz", "Kyrgyzstan"},
                {"khm", "Cambodia"},
                {"kir", "Kiribati"},
                {"kna", "Saint Kitts and Nevis"},
                {"kor", "Korea"},
                {"kwt", "Kuwait"},
                {"lao", "Lao People's Democratic Republic"},
                {"lbn", "Lebanon"},
                {"lbr", "Liberia"},
                {"lby", "Libya"},
                {"lca", "Saint Lucia"},
                {"lie", "Liechtenstein"},
                {"lka", "Sri Lanka"},
                {"lso", "Lesotho"},
                {"ltu", "Lithuania"},
                {"lux", "Luxembourg"},
                {"lva", "Latvia"},
                {"mac", "Macao"},
                {"maf", "Saint Martin"},
                {"mar", "Morocco"},
                {"mco", "Monaco"},
                {"mda", "Moldova"},
                {"mdg", "Madagascar"},
                {"mdv", "Maldives"},
                {"mex", "Mexico"},
                {"mhl", "Marshall Islands"},
                {"mkd", "North Macedonia"},
                {"mli", "Mali"},
                {"mlt", "Malta"},
                {"mmr", "Myanmar"},
                {"mne", "Montenegro"},
                {"mng", "Mongolia"},
                {"mnp", "Northern Mariana Islands"},
                {"moz", "Mozambique"},
                {"mrt", "Mauritania"},
                {"msr", "Montserrat"},
                {"mtq", "Martinique"},
                {"mus", "Mauritius"},
                {"mwi", "Malawi"},
                {"mys", "Malaysia"},
                {"myt", "Mayotte"},
                {"nam", "Namibia"},
                {"ncl", "New Caledonia"},
                {"ner", "Niger"},
                {"nfk", "Norfolk Island"},
                {"nga", "Nigeria"},
                {"nic", "Nicaragua"},
                {"niu", "Niue"},
                {"nld", "Netherlands"},
                {"nor", "Norway"},
                {"npl", "Nepal"},
                {"nru", "Nauru"},
                {"nzl", "New Zealand"},
                {"omn", "Oman"},
                {"pak", "Pakistan"},
                {"pan", "Panama"},
                {"pcn", "Pitcairn"},
                {"per", "Peru"},
                {"phl", "Philippines"},
                {"plw", "Palau"},
                {"png", "Papua New Guinea"},
                {"pol", "Poland"},
                {"pri", "Puerto Rico"},
                {"prk", "North Korea"},
                {"prt", "Portugal"},
                {"pry", "Paraguay"},
                {"pse", "Palestine"},
                {"pyf", "French Polynesia"},
                {"qat", "Qatar"},
                {"reu", "Réunion"},
                {"rou", "Romania"},
                {"rus", "Russian"},
                {"rwa", "Rwanda"},
                {"sau", "Saudi Arabia"},
                {"sdn", "Sudan"},
                {"sen", "Senegal"},
                {"sgp", "Singapore"},
                {"sgs", "South Georgia and the South Sandwich Islands"},
                {"shn", "Saint Helena, Ascension and Tristan da Cunha"},
                {"sjm", "Svalbard and Jan Mayen"},
                {"slb", "Solomon Islands"},
                {"sle", "Sierra Leone"},
                {"slv", "El Salvador"},
                {"smr", "San Marino"},
                {"som", "Somalia"},
                {"spm", "Saint Pierre and Miquelon"},
                {"srb", "Serbia"},
                {"ssd", "South Sudan"},
                {"stp", "Sao Tome and Principe"},
                {"sur", "Suriname"},
                {"svk", "Slovakia"},
                {"svn", "Slovenia"},
                {"swe", "Sweden"},
                {"swz", "Eswatini"},
                {"sxm", "Sint Maarten"},
                {"syc", "Seychelles"},
                {"syr", "Syrian Arab Republic"},
                {"tca", "Turks and Caicos Islands"},
                {"tcd", "Chad"},
                {"tgo", "Togo"},
                {"tha", "Thailand"},
                {"tjk", "Tajikistan"},
                {"tkl", "Tokelau"},
                {"tkm", "Turkmenistan"},
                {"tls", "Timor-Leste"},
                {"ton", "Tonga"},
                {"tto", "Trinidad and Tobago"},
                {"tun", "Tunisia"},
                {"tur", "Turkey"},
                {"tuv", "Tuvalu"},
                {"twn", "Taiwan, Province of China"},
                {"tza", "Tanzania"},
                {"uga", "Uganda"},
                {"ukr", "Ukraine"},
                {"umi", "United States Minor Outlying Islands"},
                {"ury", "Uruguay"},
                {"usa", "United States of America"},
                {"uzb", "Uzbekistan"},
                {"vat", "Holy See"},
                {"vct", "Saint Vincent and the Grenadines"},
                {"ven", "Venezuela"},
                {"vgb", "Virgin Islands (British)"},
                {"vir", "Virgin Islands (U.S.)"},
                {"vnm", "Viet Nam"},
                {"vut", "Vanuatu"},
                {"wlf", "Wallis and Futuna"},
                {"wsm", "Samoa"},
                {"yem", "Yemen"},
                {"zaf", "South Africa"},
                {"zmb", "Zambia"},
                {"zwe", "Zimbabwe"},
        };


        for (String[] e :entries)
        {
            iso6392b2t_diff.put(e[0], e[1]);
        }
        for (String[] e :entries_3166)
        {
            iso3166_a3.put(e[0], e[1]);
        }

    }

    /**
     * Try to convert iso639-2/b to iso639-2/t
     * For Android locale class does not support iso639-2/b, but some
     * dtv streams transfer language code with iso639-2/b type.
     * We store the difernent code list between iso639-2/b and iso639-2/t
     * @param lanCode (lowercase)
     * @return If lanCode in the store list, return the value of iso639-2t,
     * otherwise, return the lanCode self (Maybe is iso639-2/t or iso639-3
     * code, and this should be parsed by android locale)
     */
    public static String parse(String lanCode) {
        String iso2tCode = iso6392b2t_diff.get(lanCode);
        return (iso2tCode == null) ? lanCode : iso2tCode;
    }

    /**
     * Try to get country name with iso6133-1 alpha 3 country code
     * Android Locale dose not support iso6133-1 alpha 3
     * @param country_code (lowercase)
     * @return If country name
     */
    public static String getCountryNameFromCode(String country_code) {
        return iso3166_a3.get(country_code);
    }

}
