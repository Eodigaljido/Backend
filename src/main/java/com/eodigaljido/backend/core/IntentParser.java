package com.eodigaljido.backend.core;

import com.eodigaljido.backend.dto.ai.IntentResult;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.*;

@Component
public class IntentParser {

    private static final Map<String, List<String>> REGION_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, List<String>> THEME_KEYWORDS  = new LinkedHashMap<>();
    private static final Map<String, List<String>> TRAVEL_TYPE_KW  = new LinkedHashMap<>();
    private static final Set<String> NEARBY_TRIGGERS = new HashSet<>();
    private static final List<String> FREE_NEARBY_CATEGORIES = new ArrayList<>();
    private static final Map<String, String> BRAND_ALIASES = new HashMap<>();
    private static final Map<String, String> BRAND_SEARCH_ALIASES = new HashMap<>();

    private static final List<Pattern> PLACE_SUFFIX_PATTERNS = new ArrayList<>();
    private static final Pattern BRAND_PATTERN = Pattern.compile("\\b[A-Za-z][A-Za-z0-9+]{2,15}\\b");

    private static final List<String[]> KNOWN_BRAND_PATTERNS_LIST = new ArrayList<>(); // [regex, canonical]

    static {
        REGION_KEYWORDS.put("제주", Arrays.asList("성산일출봉", "한라산 등반로", "협재해수욕장", "천지연폭포", "우도"));
        REGION_KEYWORDS.put("부산", Arrays.asList("해운대해수욕장", "광안리해수욕장", "감천문화마을", "남포동 돼지국밥", "태종대"));
        REGION_KEYWORDS.put("경주", Arrays.asList("불국사", "첨성대", "경주 국립박물관", "안압지 동궁과 월지", "석굴암"));
        REGION_KEYWORDS.put("전주", Arrays.asList("전주 한옥마을", "경기전", "전주 남부시장", "전동성당", "오목대"));
        REGION_KEYWORDS.put("강릉", Arrays.asList("경포해수욕장", "정동진", "오죽헌", "강릉 중앙시장", "안목해변 커피거리"));
        REGION_KEYWORDS.put("속초", Arrays.asList("설악산 국립공원", "속초해수욕장", "청초호", "속초 중앙시장 닭강정", "영금정"));
        REGION_KEYWORDS.put("서울", Arrays.asList("경복궁", "북촌한옥마을", "남산서울타워", "홍대거리", "광장시장"));
        REGION_KEYWORDS.put("인천", Arrays.asList("인천 차이나타운", "을왕리해수욕장", "월미도", "송도센트럴파크", "강화도"));
        REGION_KEYWORDS.put("수원", Arrays.asList("수원화성", "행궁동 벽화마을", "화성행궁", "수원 팔달문시장"));
        REGION_KEYWORDS.put("춘천", Arrays.asList("남이섬", "춘천 닭갈비골목", "소양강댐", "김유정 문학촌", "의암호"));
        REGION_KEYWORDS.put("여수", Arrays.asList("여수 돌산도", "오동도", "여수 밤바다", "향일암", "여수 수산시장"));
        REGION_KEYWORDS.put("통영", Arrays.asList("통영 케이블카", "미륵산", "통영 중앙시장 꿀빵", "이순신공원", "달아공원"));
        REGION_KEYWORDS.put("담양", Arrays.asList("메타세쿼이아 가로수길", "죽녹원", "소쇄원", "담양 국수거리"));
        REGION_KEYWORDS.put("순천", Arrays.asList("순천만 국가정원", "순천만 습지", "낙안읍성", "드라마촬영장"));
        REGION_KEYWORDS.put("안동", Arrays.asList("안동 하회마을", "도산서원", "안동찜닭골목", "월영교", "봉정사"));
        REGION_KEYWORDS.put("대전", Arrays.asList("성심당", "엑스포과학공원", "유성온천", "계족산 황톳길"));
        REGION_KEYWORDS.put("대구", Arrays.asList("동성로", "서문시장", "앞산공원", "수성못", "팔공산"));
        REGION_KEYWORDS.put("광주", Arrays.asList("국립아시아문화전당", "1913 송정역시장", "충장로", "무등산"));
        REGION_KEYWORDS.put("가평", Arrays.asList("남이섬", "쁘띠프랑스", "아침고요수목원", "자라섬"));
        REGION_KEYWORDS.put("용인", Arrays.asList("에버랜드", "한국민속촌", "캐리비안 베이"));
        REGION_KEYWORDS.put("강원", Arrays.asList("설악산 국립공원", "남이섬", "강릉 경포대", "춘천 닭갈비골목", "정동진"));

        THEME_KEYWORDS.put("맛집", Arrays.asList("맛집", "음식", "먹거리", "식당", "국밥", "회", "횟집", "투어"));
        THEME_KEYWORDS.put("자연", Arrays.asList("자연", "산", "바다", "해변", "폭포", "숲", "힐링", "등산"));
        THEME_KEYWORDS.put("역사", Arrays.asList("역사", "유적", "박물관", "문화재", "유물", "고궁", "성", "서원"));
        THEME_KEYWORDS.put("액티비티", Arrays.asList("액티비티", "서핑", "래프팅", "스카이", "번지", "짚라인", "체험"));
        THEME_KEYWORDS.put("휴양", Arrays.asList("휴양", "온천", "리조트", "호텔", "스파", "힐링", "쉬"));
        THEME_KEYWORDS.put("쇼핑", Arrays.asList("쇼핑", "시장", "백화점", "쇼핑몰", "면세점"));
        THEME_KEYWORDS.put("문화", Arrays.asList("문화", "공연", "전시", "미술관", "축제"));
        THEME_KEYWORDS.put("카페", Arrays.asList("카페", "커피", "디저트", "브런치"));

        TRAVEL_TYPE_KW.put("혼행", Arrays.asList("혼자", "혼행", "솔로", "1인"));
        TRAVEL_TYPE_KW.put("커플", Arrays.asList("커플", "남자친구", "여자친구", "데이트", "연인", "둘이"));
        TRAVEL_TYPE_KW.put("가족", Arrays.asList("가족", "아이", "어린이", "초등학생", "유아", "부모님"));
        TRAVEL_TYPE_KW.put("친구", Arrays.asList("친구", "친구들", "동생", "오빠", "언니", "형"));

        NEARBY_TRIGGERS.addAll(Arrays.asList("근처", "가까운", "인근", "주변", "가까이", "옆에", "근방", "내 위치", "지금 위치", "현 위치", "현재 위치"));
        FREE_NEARBY_CATEGORIES.addAll(Arrays.asList("맛집", "카페", "커피", "식당", "음식점", "편의점", "마트", "약국", "병원", "주차장", "호텔", "숙소", "모텔", "관광지", "공원", "박물관"));

        BRAND_ALIASES.put("yse24", "yes24");
        BRAND_ALIASES.put("emart24", "이마트24");
        BRAND_SEARCH_ALIASES.put("gs25", "GS25");
        BRAND_SEARCH_ALIASES.put("cu", "CU");
        BRAND_SEARCH_ALIASES.put("emart24", "이마트24");
        BRAND_SEARCH_ALIASES.put("yes24", "yes24 서점");

        KNOWN_BRAND_PATTERNS_LIST.add(new String[]{"gs\\s*25", "GS25"});
        KNOWN_BRAND_PATTERNS_LIST.add(new String[]{"이마트\\s*24", "이마트24"});
        KNOWN_BRAND_PATTERNS_LIST.add(new String[]{"e-?\\s*mart\\s*24", "이마트24"});
        KNOWN_BRAND_PATTERNS_LIST.add(new String[]{"\\bcu\\b", "CU"});
        KNOWN_BRAND_PATTERNS_LIST.add(new String[]{"씨유", "CU"});
        KNOWN_BRAND_PATTERNS_LIST.add(new String[]{"세븐\\s*일레븐", "세븐일레븐"});
        KNOWN_BRAND_PATTERNS_LIST.add(new String[]{"7-?\\s*eleven", "세븐일레븐"});

        for (String suffix : new String[]{"역", "백화점", "마트", "서점", "공원", "시장", "센터", "호텔", "카페", "식당", "음식점", "터미널", "공항", "박물관", "미술관", "병원"}) {
            String pat = "역".equals(suffix)
                    ? "[가-힣a-zA-Z0-9]{1,12}역(?!\\s*할)"
                    : "[가-힣a-zA-Z0-9]{1,12}" + suffix;
            PLACE_SUFFIX_PATTERNS.add(Pattern.compile(pat));
        }
    }

    public IntentResult extract(String msg) {
        List<String> destinations = new ArrayList<>();
        List<String> searchKeywords = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : REGION_KEYWORDS.entrySet()) {
            if (msg.contains(entry.getKey())) {
                destinations.add(entry.getKey());
                List<String> kws = entry.getValue();
                for (int i = 0; i < Math.min(4, kws.size()); i++) searchKeywords.add(kws.get(i));
            }
        }
        searchKeywords = dedup(searchKeywords).subList(0, Math.min(5, searchKeywords.size()));

        Integer days = null;
        if (msg.contains("당일")) {
            days = 1;
        } else {
            Matcher m = Pattern.compile("(\\d+)박\\s*(\\d+)일").matcher(msg);
            if (m.find()) days = Integer.parseInt(m.group(2));
            else {
                m = Pattern.compile("(\\d+)\\s*일").matcher(msg);
                if (m.find()) days = Integer.parseInt(m.group(1));
            }
        }

        Integer budgetKrw = null;
        Matcher bm = Pattern.compile("(\\d+)\\s*만\\s*원").matcher(msg);
        if (bm.find()) budgetKrw = Integer.parseInt(bm.group(1)) * 10000;
        else {
            bm = Pattern.compile("(\\d[\\d,]*)\\s*원").matcher(msg);
            if (bm.find()) budgetKrw = Integer.parseInt(bm.group(1).replace(",", ""));
        }

        String theme = null;
        outer:
        for (Map.Entry<String, List<String>> entry : THEME_KEYWORDS.entrySet()) {
            for (String kw : entry.getValue()) {
                if (msg.contains(kw)) { theme = entry.getKey(); break outer; }
            }
        }

        String travelType = null;
        outer2:
        for (Map.Entry<String, List<String>> entry : TRAVEL_TYPE_KW.entrySet()) {
            for (String kw : entry.getValue()) {
                if (msg.contains(kw)) { travelType = entry.getKey(); break outer2; }
            }
        }

        boolean nearby = NEARBY_TRIGGERS.stream().anyMatch(msg::contains);
        List<String> freeKeywords = FREE_NEARBY_CATEGORIES.stream().filter(msg::contains).toList();

        List<String> finalSearchKeywords = new ArrayList<>(searchKeywords);
        if (nearby && finalSearchKeywords.isEmpty() && !freeKeywords.isEmpty()) {
            finalSearchKeywords.addAll(freeKeywords.subList(0, Math.min(3, freeKeywords.size())));
        } else if (!freeKeywords.isEmpty() && !destinations.isEmpty()) {
            String prefix = destinations.get(0);
            for (String kw : freeKeywords.subList(0, Math.min(3, freeKeywords.size()))) {
                finalSearchKeywords.add(prefix + " " + kw);
            }
        }

        List<String> specificPlaces = extractSpecificPlaces(msg, destinations);
        if (!specificPlaces.isEmpty()) {
            finalSearchKeywords = new ArrayList<>();
            String prefix = destinations.isEmpty() ? "" : destinations.get(0);
            for (String p : specificPlaces.subList(0, Math.min(5, specificPlaces.size()))) {
                finalSearchKeywords.add((!prefix.isEmpty() && !p.contains(prefix)) ? prefix + " " + p : p);
            }
        }

        return IntentResult.builder()
                .destinations(destinations)
                .theme(theme)
                .days(days)
                .budgetKrw(budgetKrw)
                .travelType(travelType)
                .searchKeywords(finalSearchKeywords)
                .nearby(nearby)
                .freeKeywords(freeKeywords)
                .orderedPlaces(specificPlaces.subList(0, Math.min(5, specificPlaces.size())))
                .build();
    }

    private List<String> extractSpecificPlaces(String msg, List<String> destinations) {
        List<int[]> hits = new ArrayList<>(); // [start, canonical]
        List<String> hitNames = new ArrayList<>();

        for (String[] bp : KNOWN_BRAND_PATTERNS_LIST) {
            Matcher m = Pattern.compile(bp[0], Pattern.CASE_INSENSITIVE).matcher(msg);
            while (m.find()) {
                hits.add(new int[]{m.start(), hits.size()});
                hitNames.add(bp[1]);
            }
        }
        for (Pattern p : PLACE_SUFFIX_PATTERNS) {
            Matcher m = p.matcher(msg);
            while (m.find()) {
                hits.add(new int[]{m.start(), hits.size()});
                hitNames.add(m.group(0));
            }
        }
        Matcher bm = BRAND_PATTERN.matcher(msg);
        while (bm.find()) {
            String brand = bm.group(0);
            brand = BRAND_ALIASES.getOrDefault(brand.toLowerCase(), brand);
            if (!brand.toLowerCase().matches("from|to|the|and|for|with|near")) {
                hits.add(new int[]{bm.start(), hits.size()});
                hitNames.add(brand);
            }
        }

        hits.sort(Comparator.comparingInt(a -> a[0]));

        List<String> result = new ArrayList<>();
        for (int[] hit : hits) {
            String place = hitNames.get(hit[1]);
            String norm = BRAND_SEARCH_ALIASES.getOrDefault(place.toLowerCase(), place);
            String normKey = norm.replaceAll("\\s+", "").toLowerCase();

            boolean skip = false;
            for (String existing : result) {
                String existingKey = existing.replaceAll("\\s+", "").toLowerCase();
                if (normKey.equals(existingKey) || (normKey.length() < existingKey.length() && existingKey.contains(normKey))) {
                    skip = true; break;
                }
            }
            if (!skip) {
                result.removeIf(e -> {
                    String ek = e.replaceAll("\\s+", "").toLowerCase();
                    return ek.length() < normKey.length() && normKey.contains(ek);
                });
                result.add(norm);
            }
        }
        return result;
    }

    private <T> List<T> dedup(List<T> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }
}
