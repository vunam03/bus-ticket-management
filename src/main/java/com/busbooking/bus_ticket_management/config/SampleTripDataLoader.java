package com.busbooking.bus_ticket_management.config;

import com.busbooking.bus_ticket_management.model.BusCompany;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.utils.DateUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Nạp dữ liệu mẫu để thử Module 3 khi Module 1 (quản lý chuyến xe) chưa có giao diện nhập liệu.
 * <p>
 * MẶC ĐỊNH TẮT. Bật bằng biến môi trường APP_SEED_ENABLED=true (hoặc --app.seed.enabled=true).
 * Chỉ chạy khi bảng bus_trips đang rỗng. Sinh chuyến cho 14 ngày tới, 11 tuyến x 2 chiều.
 * Dùng Random cố định nên dữ liệu ổn định giữa các lần chạy.
 * <p>
 * Dùng EntityManager trực tiếp để không phụ thuộc BusCompanyRepository (thuộc Module 1).
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class SampleTripDataLoader implements CommandLineRunner {

    private static final int DAYS = 14;
    private static final int[] HOURS = {5, 6, 7, 8, 9, 11, 13, 15, 17, 19, 20, 21, 22, 23};
    private static final String[] BUS_TYPES = {"Giường nằm", "Limousine", "Ghế ngồi"};

    /** {tên, điện thoại, địa chỉ, điểm đánh giá} */
    private static final String[][] COMPANIES = {
            {"Phương Trang", "19006067", "Bến xe Miền Đông, TP. Hồ Chí Minh", "4.6"},
            {"Hoàng Long", "19001234", "Bến xe Giáp Bát, Hà Nội", "4.4"},
            {"Thành Bưởi", "19006079", "Bến xe Miền Tây, TP. Hồ Chí Minh", "4.5"},
            {"Kumho Samco", "19006069", "Bến xe Trung tâm Đà Nẵng", "4.2"},
            {"Sao Việt", "19001258", "Bến xe Gia Lâm, Hà Nội", "4.0"},
            {"Hải Vân", "19001500", "Bến xe Đà Nẵng", "3.9"},
            {"Mai Linh Express", "19001099", "Bến xe Nước Ngầm, Hà Nội", "4.7"},
            {"Cúc Tùng", "19001108", "Bến xe Lạng Sơn", "3.6"}
    };

    private record Route(String from, String to, int basePrice, boolean longHaul) {
    }

    private static final List<Route> ROUTES = List.of(
            new Route("Hà Nội", "Hải Phòng", 120_000, false),
            new Route("Hà Nội", "Sa Pa", 280_000, false),
            new Route("Hà Nội", "Ninh Bình", 100_000, false),
            new Route("Hà Nội", "Vinh", 250_000, true),
            new Route("Hà Nội", "Đà Nẵng", 450_000, true),
            new Route("Đà Nẵng", "Huế", 120_000, false),
            new Route("TP. Hồ Chí Minh", "Đà Lạt", 300_000, true),
            new Route("TP. Hồ Chí Minh", "Vũng Tàu", 130_000, false),
            new Route("TP. Hồ Chí Minh", "Cần Thơ", 150_000, false),
            new Route("TP. Hồ Chí Minh", "Nha Trang", 330_000, true),
            new Route("Đà Nẵng", "TP. Hồ Chí Minh", 600_000, true));

    @PersistenceContext
    private EntityManager entityManager;

    private final PlatformTransactionManager transactionManager;

    public SampleTripDataLoader(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void run(String... args) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> seed());
    }

    private void seed() {
        Long existing = entityManager.createQuery("SELECT COUNT(t) FROM BusTrip t", Long.class).getSingleResult();
        if (existing > 0) {
            return;
        }

        List<BusCompany> companies = new ArrayList<>();
        for (String[] data : COMPANIES) {
            BusCompany company = new BusCompany();
            company.setName(data[0]);
            company.setPhoneNumber(data[1]);
            company.setAddress(data[2]);
            company.setRating(Double.valueOf(data[3]));
            entityManager.persist(company);
            companies.add(company);
        }

        Random random = new Random(2026);
        LocalDate today = DateUtils.today();
        int count = 0;
        for (Route route : ROUTES) {
            for (int direction = 0; direction < 2; direction++) {
                String from = direction == 0 ? route.from() : route.to();
                String to = direction == 0 ? route.to() : route.from();
                for (int day = 0; day < DAYS; day++) {
                    int tripsPerDay = 3 + random.nextInt(3);
                    for (int i = 0; i < tripsPerDay; i++) {
                        entityManager.persist(createTrip(random, route, from, to, today.plusDays(day), companies));
                        count++;
                    }
                }
            }
        }
        System.out.println("[SampleTripDataLoader] Đã nạp " + companies.size() + " nhà xe và " + count + " chuyến xe mẫu.");
    }

    private BusTrip createTrip(Random random, Route route, String from, String to,
                               LocalDate date, List<BusCompany> companies) {
        int hour = HOURS[random.nextInt(HOURS.length)];
        LocalDateTime departure = date.atTime(hour, random.nextInt(4) * 15);

        String busType = pickBusType(random, route.longHaul());
        int totalSeats = switch (busType) {
            case "Limousine" -> 22;
            case "Giường nằm" -> 36;
            default -> 45;
        };
        double multiplier = switch (busType) {
            case "Limousine" -> 1.6;
            case "Giường nằm" -> 1.2;
            default -> 1.0;
        };
        long rawPrice = Math.round(route.basePrice() * multiplier * (0.9 + random.nextDouble() * 0.25));
        long price = Math.max(50_000, Math.round(rawPrice / 10_000.0) * 10_000);

        BusTrip trip = new BusTrip();
        trip.setRouteName(from + " - " + to);
        trip.setDepartureLocation(from);
        trip.setArrivalLocation(to);
        trip.setDepartureTime(departure);
        trip.setTicketPrice(BigDecimal.valueOf(price));
        trip.setTotalSeats(totalSeats);
        trip.setFloorCount(2);                       // giống BusTripService#createTrip
        trip.setBasePrice(BigDecimal.valueOf(price)); // giống BusTripService#createTrip
        trip.setBusType(busType);
        trip.setBusCompany(companies.get(random.nextInt(companies.size())));
        return trip;
    }

    /** Tuyến dài ưu tiên giường nằm/limousine; tuyến ngắn ưu tiên ghế ngồi. */
    private String pickBusType(Random random, boolean longHaul) {
        int roll = random.nextInt(100);
        if (longHaul) {
            return roll < 65 ? BUS_TYPES[0] : BUS_TYPES[1];
        }
        if (roll < 50) {
            return BUS_TYPES[2];
        }
        return roll < 80 ? BUS_TYPES[1] : BUS_TYPES[0];
    }
}
