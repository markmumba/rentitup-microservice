package com.rentitup.catalog_service.entities;

import com.rentitup.catalog_service.common.entites.BaseEntity;
import com.rentitup.catalog_service.enums.PriceCalculationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories" )
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
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
	@JsonIgnore
	private List<MachineEntity> machines = new ArrayList<>();

	@Formula("(SELECT COUNT(*) FROM catalog.machines m WHERE m.category_id = id)")
	private int machineCount;

}
