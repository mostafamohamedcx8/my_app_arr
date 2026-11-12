package com.myapp.aqdeeha.modules.catalog.mapper;

import com.myapp.aqdeeha.common.storage.FileStorageService;
import com.myapp.aqdeeha.modules.catalog.dto.CatalogResponses.CategoryResponse;
import com.myapp.aqdeeha.modules.catalog.dto.CatalogResponses.ShopResponse;
import com.myapp.aqdeeha.modules.catalog.entity.Category;
import com.myapp.aqdeeha.modules.catalog.entity.Shop;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class CatalogMapper {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final FileStorageService storage;
    private final Clock clock;
    private final ZoneId displayZone;

    public CatalogMapper(FileStorageService storage, Clock clock, @Value("${app.timezone.display}") String displayZone) {
        this.storage = storage;
        this.clock = clock;
        this.displayZone = ZoneId.of(displayZone);
    }

    public CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getIconKey(), storage.publicUrl(c.getIconUrl()),
                c.getColor(), c.getSortOrder(), c.isActive(), c.getCreatedAt());
    }

    public ShopResponse toResponse(Shop s, String categoryName) {
        LocalTime nowInCairo = LocalTime.now(clock.withZone(displayZone));
        return new ShopResponse(s.getId(), s.getCategoryId(), categoryName, s.getName(),
                storage.publicUrl(s.getPhotoUrl()), s.getStreet(), s.getLat(), s.getLng(),
                s.getOpensAt().format(HH_MM), s.getClosesAt().format(HH_MM), s.isOpen(),
                Shop.isOpenAt(s.isOpen(), s.getOpensAt(), s.getClosesAt(), nowInCairo));
    }
}
