package com.server.server.seeds.development;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.config.properties.AppProperties;
import com.server.server.dto.common.EntityReference;
import com.server.server.dto.geography.CityCreateRequest;
import com.server.server.dto.tourmanagement.MealPlanCreateRequest;
import com.server.server.dto.tourmanagement.SeatUpdateRequest;
import com.server.server.dto.tourmanagement.TourCreateRequest;
import com.server.server.dto.tourmanagement.TransportationCreateRequest;
import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.MealDietaryType;
import com.server.server.enums.tourmanagement.SpiceLevel;
import com.server.server.enums.tourmanagement.TransportationType;
import com.server.server.models.geography.City;
import com.server.server.models.geography.Country;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Transportation;
import com.server.server.models.user.User;
import com.server.server.repositories.geography.CityRepository;
import com.server.server.repositories.geography.CountryRepository;
import com.server.server.repositories.tourmanagement.SeatRepository;
import com.server.server.repositories.tourmanagement.TourRepository;
import com.server.server.repositories.user.UserRepository;
import com.server.server.services.geography.CityService;
import com.server.server.services.tourmanagement.MealPlanService;
import com.server.server.services.tourmanagement.SeatService;
import com.server.server.services.tourmanagement.TourService;
import com.server.server.services.tourmanagement.TourStatusService;
import com.server.server.services.tourmanagement.TransportationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Demo catalog for a seeded environment: two vehicles with kids, premium and standard seats, a few meals,
 * and four bookable tours of the seeded agency, dated from today. Runs after {@link DataSeeder} under the
 * same switch, never under the {@code prod} profile, and only while there are no tours. Everything goes
 * through the services as the seeded owner, so the same rules apply as for the API.
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class DemoCatalogSeeder implements CommandLineRunner {

    private static final String PRODUCTION_PROFILE = "prod";
    private static final String OWNER_EMAIL = "owner@funtravel.com";
    private static final double PREMIUM_SEAT_PRICE = 25;

    private record DemoTour(String number, String title, String description, double price, double discount,
            int startInDays, int days, int capacity, String fromCountry, String fromCity, String toCountry,
            String toCity, boolean bigBus) {
    }

    private static final List<DemoTour> TOURS = List.of(
            new DemoTour("DEMO-BAH-01", "Bahrain Heritage Day",
                    "Bab Al Bahrain, the Bahrain Fort and the old souq, with lunch in Muharraq.",
                    45, 0, 10, 1, 12, "BH", "Manama", "BH", "Muharraq", false),
            new DemoTour("DEMO-KHO-01", "Al Khobar Weekend Escape",
                    "Over the King Fahd Causeway for the corniche, the Dhahran science centre and the beach.",
                    120, 0, 20, 2, 12, "BH", "Manama", "SA", "Al Khobar", false),
            new DemoTour("DEMO-DXB-01", "Dubai City Lights",
                    "Downtown, the creek by abra, the desert safari and a night at the marina.",
                    320, 0, 30, 4, 30, "BH", "Manama", "AE", "Dubai", true),
            new DemoTour("DEMO-MCT-01", "Muscat Mountains and Wadis",
                    "Old Muscat, Wadi Shab, Jebel Akhdar and the Nizwa fort. Now on sale.",
                    450, 50, 45, 5, 30, "BH", "Manama", "OM", "Muscat", true));

    private final AppProperties appProperties;
    private final Environment environment;
    private final UserRepository userRepository;
    private final TourRepository tourRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final SeatRepository seatRepository;
    private final CityService cityService;
    private final TransportationService transportationService;
    private final SeatService seatService;
    private final MealPlanService mealPlanService;
    private final TourService tourService;
    private final TourStatusService tourStatusService;

    @Override
    @Transactional
    public void run(String... args) {
        if (!appProperties.seed().enabled()
                || Arrays.asList(environment.getActiveProfiles()).contains(PRODUCTION_PROFILE)
                || tourRepository.count() > 0) {
            return;
        }
        Optional<User> owner = userRepository.findByEmailIgnoreCase(OWNER_EMAIL);
        if (owner.isEmpty() || owner.get().getAgency() == null) {
            log.info("No seeded agency owner. Skipping demo catalog.");
            return;
        }
        if (TOURS.stream().anyMatch(t -> countryRepository.findByCountryCodeIgnoreCase(t.toCountry()).isEmpty())) {
            log.warn("Countries are not imported yet. Skipping demo catalog.");
            return;
        }
        User user = owner.get();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority(user.getUserType().name()))));
        try {
            createCatalog(user.getAgency().getId());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void createCatalog(Integer agencyId) {
        Transportation van = vehicle("DEMO-VAN-12", "Gulf Coaches", 12, agencyId, Map.of("KIDS_CHAIR", 2, "PREMIUM_RECLINER", 2));
        Transportation bus = vehicle("DEMO-BUS-30", "Desert Line", 30, agencyId, Map.of("KIDS_CHAIR", 6, "PREMIUM_RECLINER", 4));
        List<Integer> meals = List.of(
                meal("Machboos chicken", 6.5, "Spiced rice with chicken", Set.of(MealDietaryType.STANDARD), SpiceLevel.MEDIUM),
                meal("Falafel wrap", 4, "Falafel, tahini and salad", Set.of(MealDietaryType.VEGETARIAN, MealDietaryType.VEGAN), SpiceLevel.MILD),
                meal("Kids lunch box", 3, "Sandwich, juice and fruit", Set.of(MealDietaryType.STANDARD), SpiceLevel.NONE),
                meal("Grilled fish platter", 9, "Hammour with rice and salad", Set.of(MealDietaryType.GLUTEN_FREE), SpiceLevel.MILD));
        for (DemoTour demo : TOURS) {
            Country from = countryRepository.findByCountryCodeIgnoreCase(demo.fromCountry()).orElseThrow();
            Country to = countryRepository.findByCountryCodeIgnoreCase(demo.toCountry()).orElseThrow();
            LocalDate start = LocalDate.now().plusDays(demo.startInDays());
            var tour = tourService.create(new TourCreateRequest(demo.number(), demo.title(), demo.description(),
                    demo.price(), demo.discount(), demo.days(), start, start.plusDays(demo.days() - 1L),
                    demo.capacity(), from.getId(), to.getId(), city(from, demo.fromCity()), city(to, demo.toCity()),
                    List.of(to.getId()), (demo.bigBus() ? bus : van).getId(), meals));
            tourStatusService.updateStatus(tour.getId(), GenericStatus.APPROVED);
            tourStatusService.updateStatus(tour.getId(), GenericStatus.ACTIVE);
        }
        log.info("Demo catalog created: {} tours, 2 vehicles, {} meals.", TOURS.size(), meals.size());
    }

    private Transportation vehicle(String number, String provider, int seats, Integer agencyId, Map<String, Integer> layout) {
        Transportation vehicle = transportationService.create(new TransportationCreateRequest(number, number, TransportationType.BUS,
                provider, seats, agencyId, null, layout));
        for (Seat seat : seatRepository.findByTransportation_IdOrderByIdAsc(vehicle.getId())) {
            if (seat.getChairType() == ChairType.PREMIUM_RECLINER) {
                seatService.updateSeat(seat.getId(), new SeatUpdateRequest(null, null, PREMIUM_SEAT_PRICE));
            }
        }
        return vehicle;
    }

    private Integer meal(String name, double price, String description, Set<MealDietaryType> diets, SpiceLevel spice) {
        return mealPlanService.createMeal(new MealPlanCreateRequest(name, price, description, diets, spice, null)).getId();
    }

    private Integer city(Country country, String name) {
        return cityRepository.findByCountryId(country.getId()).stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .map(City::getId)
                .findFirst()
                .orElseGet(() -> cityService.createCity(new CityCreateRequest(name, new EntityReference(country.getId()))).getId());
    }
}
