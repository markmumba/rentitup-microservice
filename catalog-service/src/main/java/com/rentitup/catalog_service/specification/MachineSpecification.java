package com.rentitup.catalog_service.specification;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.catalog_service.enums.MachineCondition;
import com.rentitup.catalog_service.enums.MachineStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class MachineSpecification {

	// ==================== Basic Filters ====================

	public static Specification<MachineEntity> hasCategory(UUID categoryId) {
		return (root, query, cb) -> {
			if (categoryId == null) return cb.conjunction();
			Join<MachineEntity, CategoryEntity> join = root.join("category", JoinType.INNER);
			return cb.equal(join.get("id"), categoryId);
		};
	}

	public static Specification<MachineEntity> hasStatus(MachineStatus status) {
		return (root, query, cb) -> {
			if (status == null) return cb.conjunction();
			return cb.equal(root.get("status"), status);
		};
	}

	public static Specification<MachineEntity> hasOwner(UUID ownerId) {
		return (root, query, cb) -> {
			if (ownerId == null) return cb.conjunction();
			return cb.equal(root.get("ownerId"), ownerId);
		};
	}

	public static Specification<MachineEntity> hasMinCondition(MachineCondition minCondition) {
		return (root, query, cb) -> {
			if (minCondition == null) return cb.conjunction();
			return cb.lessThanOrEqualTo(root.get("condition"), minCondition);
		};
	}

	public static Specification<MachineEntity> isAvailable() {
		return (root, query, cb) -> cb.equal(root.get("available"), true);
	}

	public static Specification<MachineEntity> notDeleted() {
		return (root, query, cb) -> cb.equal(root.get("deleted"), false);
	}

	// ==================== Price & Rating Filters ====================

	public static Specification<MachineEntity> priceRange(BigDecimal minPrice, BigDecimal maxPrice) {
		return (root, query, cb) -> {
			if (minPrice == null && maxPrice == null) return cb.conjunction();
			if (minPrice != null && maxPrice != null) {
				return cb.between(root.get("basePrice"), minPrice, maxPrice);
			}
			if (minPrice != null) {
				return cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice);
			}
			return cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice);
		};
	}

	public static Specification<MachineEntity> hasMinRating(Double minRating) {
		return (root, query, cb) -> {
			if (minRating == null) return cb.conjunction();
			return cb.greaterThanOrEqualTo(root.get("averageRating"), BigDecimal.valueOf(minRating));
		};
	}

	// ==================== Text Search ====================

	public static Specification<MachineEntity> searchQuery(String query) {
		return (root, query_, cb) -> {
			if (query == null || query.isBlank()) return cb.conjunction();
			String pattern = "%" + query.toLowerCase() + "%";
			return cb.or(
					cb.like(cb.lower(root.get("name")), pattern),
					cb.like(cb.lower(root.get("description")), pattern)
			);
		};
	}

	// ==================== Location Filter ====================

	/**
	 * Bounding box approach for nearby search.
	 * Creates a rectangular area around the coordinates.
	 */
	public static Specification<MachineEntity> isNearby(BigDecimal latitude, BigDecimal longitude, Double radiusKm) {
		return (root, query, cb) -> {
			if (latitude == null || longitude == null || radiusKm == null) return cb.conjunction();

			// 1 degree latitude ≈ 111km
			double latDelta = radiusKm / 111.0;
			// Longitude varies by latitude
			double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude.doubleValue())));

			BigDecimal minLat = latitude.subtract(BigDecimal.valueOf(latDelta));
			BigDecimal maxLat = latitude.add(BigDecimal.valueOf(latDelta));
			BigDecimal minLng = longitude.subtract(BigDecimal.valueOf(lngDelta));
			BigDecimal maxLng = longitude.add(BigDecimal.valueOf(lngDelta));

			return cb.and(
					cb.between(root.get("latitude"), minLat, maxLat),
					cb.between(root.get("longitude"), minLng, maxLng)
			);
		};
	}
}
