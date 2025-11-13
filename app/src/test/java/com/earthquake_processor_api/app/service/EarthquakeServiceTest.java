package com.earthquake_processor_api.app.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class EarthquakeServiceTest {

    private final EarthquakeService service = new EarthquakeService(null, null, null);

    // ======== isSignificantEarthquake Parameterized Tests ========

    private static class SignificanceTestCase {
        Double magnitude;
        Boolean tsunami;
        String alertLevel;
        Double population100km;
        Double distanceToNearestCityKm;
        boolean expected;

        SignificanceTestCase(Double magnitude, Boolean tsunami, String alertLevel,
                             Double population100km, Double distanceToNearestCityKm, boolean expected) {
            this.magnitude = magnitude;
            this.tsunami = tsunami;
            this.alertLevel = alertLevel;
            this.population100km = population100km;
            this.distanceToNearestCityKm = distanceToNearestCityKm;
            this.expected = expected;
        }
    }

    static Stream<SignificanceTestCase> significanceProvider() {
        return Stream.of(
                new SignificanceTestCase(6.1, false, null, null, null, true),
                new SignificanceTestCase(5.5, false, null, null, 20.0, true),
                new SignificanceTestCase(4.5, false, null, 1_500_000.0, 10.0, true),
                new SignificanceTestCase(3.0, true, null, null, null, true),
                new SignificanceTestCase(3.0, false, "red", null, null, true),
                new SignificanceTestCase(3.0, false, null, 5_000_001.0, null, true),
                new SignificanceTestCase(3.0, false, null, 1000.0, 50.0, false),
                new SignificanceTestCase(null, false, null, null, null, false)
        );
    }

    @ParameterizedTest
    @MethodSource("significanceProvider")
    void testIsSignificantEarthquake(SignificanceTestCase tc) {
        boolean result = service.isSignificantEarthquake(
                tc.magnitude, tc.tsunami, tc.alertLevel, tc.population100km, tc.distanceToNearestCityKm
        );
        assertEquals(tc.expected, result,
                () -> String.format("Failed for magnitude=%s, tsunami=%s, alert=%s, pop=%s, distance=%s",
                        tc.magnitude, tc.tsunami, tc.alertLevel, tc.population100km, tc.distanceToNearestCityKm));
    }

    // ======== extractNearestCity Parameterized Tests ========

    private static class NearestCityTestCase {
        String place;
        String expected;

        NearestCityTestCase(String place, String expected) {
            this.place = place;
            this.expected = expected;
        }
    }

    static Stream<NearestCityTestCase> nearestCityProvider() {
        return Stream.of(
                new NearestCityTestCase("16 km S of Volcano, Hawaii", "Volcano, Hawaii"),
                new NearestCityTestCase("54 km NW of San Antonio, Chile", "San Antonio, Chile"),
                new NearestCityTestCase("Near the coast of Central Chile", "Central Chile"),
                new NearestCityTestCase(null, null),
                new NearestCityTestCase("Unknown location", "Unknown location")
        );
    }

    @ParameterizedTest
    @MethodSource("nearestCityProvider")
    void testExtractNearestCity(NearestCityTestCase tc) {
        String result = service.extractNearestCity(tc.place);
        assertEquals(tc.expected, result);
    }

    // ======== extractDistanceToNearestCityKm Parameterized Tests ========

    private static class DistanceTestCase {
        String place;
        Double expected;

        DistanceTestCase(String place, Double expected) {
            this.place = place;
            this.expected = expected;
        }
    }

    static Stream<DistanceTestCase> distanceProvider() {
        return Stream.of(
                new DistanceTestCase("16 km S of Volcano, Hawaii", 16.0),
                new DistanceTestCase("54 km NW of San Antonio, Chile", 54.0),
                new DistanceTestCase("10 mi W of Los Angeles, California", 16.0934),
                new DistanceTestCase("Near the coast of Central Chile", null),
                new DistanceTestCase(null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("distanceProvider")
    void testExtractDistanceToNearestCityKm(DistanceTestCase tc) {
        Double result = service.extractDistanceToNearestCityKm(tc.place);
        if (tc.expected == null) {
            assertNull(result);
        } else {
            assertEquals(tc.expected, result, 0.0001); // allow minor floating-point differences
        }
    }
}

