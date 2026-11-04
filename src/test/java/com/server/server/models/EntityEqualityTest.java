package com.server.server.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.server.server.models.agency.Agency;
import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.models.user.User;

class EntityEqualityTest {

    @Test
    void bidirectionalRelationsDoNotRecurseInHashCodeOrToString() {
        Agency agency = new Agency();
        agency.setId(1);
        User owner = new User();
        owner.setId(2);
        owner.setAgency(agency);
        agency.setAgencyOwner(owner);
        agency.setEmployees(List.of(owner));
        MealPlan meal = new MealPlan();
        meal.setId(3);
        meal.setAgency(agency);

        Set<MealPlan> meals = new HashSet<>(List.of(meal));

        assertThat(meals).contains(meal);
        assertThat(agency.toString()).contains("Agency");
    }

    @Test
    void equalityIsByTypeAndId() {
        MealPlan first = new MealPlan();
        first.setId(7);
        MealPlan second = new MealPlan();
        second.setId(7);
        MealPlan unsaved = new MealPlan();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(unsaved).isNotEqualTo(new MealPlan());
    }
}
