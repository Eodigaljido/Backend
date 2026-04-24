package com.eodigaljido.backend.service;

/**
 * KMA Lambert Conformal Conic projection: lat/lon → nx/ny grid coordinates.
 * Ported from KMA VilageFcst API guide C code.
 */
public class GeoGridConverter {

    private static final double DEGRAD = Math.PI / 180.0;

    // KMA projection constants
    private static final double RE_KM = 6371.00877;
    private static final double GRID_KM = 5.0;
    private static final double SLAT1_DEG = 30.0;
    private static final double SLAT2_DEG = 60.0;
    private static final double OLON_DEG = 126.0;
    private static final double OLAT_DEG = 38.0;
    private static final double XO = 43.0;
    private static final double YO = 136.0;

    private static final double re;
    private static final double sn;
    private static final double sf;
    private static final double ro;

    static {
        re = RE_KM / GRID_KM;

        double slat1 = SLAT1_DEG * DEGRAD;
        double slat2 = SLAT2_DEG * DEGRAD;
        double olat  = OLAT_DEG  * DEGRAD;

        // sn = log(cos slat1 / cos slat2) / log(tan(π/4+slat2/2) / tan(π/4+slat1/2))
        double tanRatio = Math.tan(Math.PI * 0.25 + slat2 * 0.5)
                        / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(tanRatio);

        sf = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), sn)
                * Math.cos(slat1) / sn;

        ro = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), sn);
    }

    /**
     * @return int[]{nx, ny} KMA grid coordinates
     */
    public static int[] convert(double latDeg, double lonDeg) {
        double lat = latDeg * DEGRAD;
        double lon = lonDeg * DEGRAD;
        double olon = OLON_DEG * DEGRAD;

        double ra = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + lat * 0.5), sn);

        double theta = lon - olon;
        if (theta >  Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) (ra * Math.sin(theta) + XO + 1.5);
        int ny = (int) (ro - ra * Math.cos(theta) + YO + 1.5);
        return new int[]{nx, ny};
    }

    private GeoGridConverter() {}
}
