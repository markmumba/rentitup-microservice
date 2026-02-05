package com.rentitup.catalog_service.entities;

import com.rentitup.catalog_service.common.entites.BaseEntity;
import com.rentitup.catalog_service.enums.PriceCalculationType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories" )
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryEntity extends BaseEntity {
	@Column(nullable = false, unique = true)
	private String name;

	private String description;

	@Column(name = "icon_url")
	private String iconUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "default_price_type")
	@Builder.Default
	private PriceCalculationType defaultPriceType = PriceCalculationType.DAILY;

	@OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
	@Builder.Default
	private List<MachineEntity> machines = new ArrayList<>();

	public int getMachineCount() {
		return machines != null ? machines.size() : 0;
	}

}
