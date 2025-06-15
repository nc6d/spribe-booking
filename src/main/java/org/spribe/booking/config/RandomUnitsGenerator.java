package org.spribe.booking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spribe.booking.dto.UnitRequest;
import org.spribe.booking.dto.UnitResponse;
import org.spribe.booking.model.enumeration.AccommodationType;
import org.spribe.booking.repository.UnitRepository;
import org.spribe.booking.service.UnitService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RandomUnitsGenerator implements ApplicationRunner, DisposableBean {
    private final UnitRepository unitRepository;
    private final Random random = new Random();
    private final List<UUID> generatedUnitIds = new ArrayList<>();

    private final ApplicationContext applicationContext;

    @Value("${data-initializer.enabled:false}")
    private boolean enabled;

    @Value("${data-initializer.cleanup-on-shutdown:false}")
    private boolean cleanupOnShutdown;

    @Value("${data-initializer.number-of-units:90}")
    private int numberOfUnits;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Data initializer is disabled");
            return;
        }

        log.info("Generating {} random units", numberOfUnits);

        UnitService unitService = applicationContext.getBean(UnitService.class); // proxy-aware
        UUID systemUser = UUID.fromString("00000000-0000-0000-0000-000000000001");

        AccommodationType type = AccommodationType.values()[random.nextInt(AccommodationType.values().length)];
        int numberOfRooms = random.nextInt(5) + 1; // 1-5 rooms
        int floor = random.nextInt(20) + 1; // 1-20 floors
        BigDecimal basePrice = BigDecimal.valueOf(random.nextDouble(50, 500))
                .setScale(2, RoundingMode.HALF_UP);

        for (int i = 0; i < 90; i++) {
            UnitRequest request = new UnitRequest();
            request.setNumberOfRooms(numberOfRooms);
            request.setType(type);
            request.setFloor(floor);
            request.setBasePrice(basePrice);
            request.setDescription(generateDescription(type, numberOfRooms, floor));

            UnitResponse unitResponse = unitService.createUnit(request, systemUser);
            generatedUnitIds.add(unitResponse.getId());
        }
        
        log.info("Generated {} units", generatedUnitIds.size());
    }

    private String generateDescription(AccommodationType type, int rooms, int floor) {
        return String.format("%s with %d %s on the %d%s floor.",
                type.name().toLowerCase(),
                rooms,
                rooms == 1 ? "room" : "rooms",
                floor,
                getFloorSuffix(floor));
    }

    private String getFloorSuffix(int floor) {
        if (floor >= 11 && floor <= 13) {
            return "th";
        }
        return switch (floor % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    @Override
    public void destroy() {
        if (!cleanupOnShutdown) {
            log.info("Cleanup on shutdown is disabled");
            return;
        }

        log.info("Cleaning up {} generated units", generatedUnitIds.size());
        
        for (UUID unitId : generatedUnitIds) {
            unitRepository.deleteById(unitId);
        }
        
        generatedUnitIds.clear();
        log.info("Cleanup completed");
    }
} 