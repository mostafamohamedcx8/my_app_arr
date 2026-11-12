package com.myapp.aqdeeha.modules.courier.repository;

import com.myapp.aqdeeha.modules.courier.entity.CourierProfile;
import com.myapp.aqdeeha.modules.user.entity.User;

/** A courier = its {@code users} row + its {@code courier_profiles} row (both managed entities). */
public record CourierRow(CourierProfile profile, User user) {
}
