package com.example.onboarding.application.service;

import com.example.onboarding.application.domain.BusinessPartner;
import com.example.onboarding.application.domain.BusinessPartnerOnboarded;
import com.example.onboarding.application.domain.PartnerId;
import com.example.onboarding.application.port.out.LoadPartnerPort;
import com.example.onboarding.application.port.out.PublishPartnerEventPort;
import com.example.onboarding.application.port.out.SavePartnerPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Hand-written fake for all outgoing ports. Small ports make fakes this cheap, and a fake tests behaviour
 * ("is the partner stored?") rather than interactions ("was save() called?").
 */
class InMemoryPartnerStore implements LoadPartnerPort, SavePartnerPort, PublishPartnerEventPort {

    final List<BusinessPartner> partners = new ArrayList<>();
    final List<BusinessPartnerOnboarded> events = new ArrayList<>();

    @Override
    public void save(BusinessPartner partner) {
        partners.add(partner);
    }

    @Override
    public Optional<BusinessPartner> findById(PartnerId id) {
        return partners.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    @Override
    public List<BusinessPartner> findAll() {
        return List.copyOf(partners);
    }

    @Override
    public List<BusinessPartner> findByCountry(String country) {
        return partners.stream().filter(p -> country.equals(p.data().address().country())).toList();
    }

    @Override
    public List<BusinessPartner> findByIban(String iban) {
        return partners.stream().filter(p -> iban.equals(p.data().iban())).toList();
    }

    @Override
    public void publish(BusinessPartnerOnboarded event) {
        events.add(event);
    }
}
