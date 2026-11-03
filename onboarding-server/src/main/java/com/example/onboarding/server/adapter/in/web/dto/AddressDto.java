package com.example.onboarding.server.adapter.in.web.dto;

import com.example.partnerquality.Address;

public record AddressDto(String street, String postalCode, String city, String country) {

    public static AddressDto from(Address address) {
        return new AddressDto(address.street(), address.postalCode(), address.city(), address.country());
    }

    public Address toDomain() {
        return new Address(street, postalCode, city, country);
    }
}
