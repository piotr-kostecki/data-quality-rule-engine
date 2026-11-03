package com.example.onboarding.server.adapter.out.persistence;

import com.example.onboarding.application.domain.BusinessPartner;
import com.example.onboarding.application.domain.PartnerId;
import com.example.onboarding.application.port.out.LoadPartnerPort;
import com.example.onboarding.application.port.out.SavePartnerPort;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Driven adapter implementing the persistence ports with a thread-safe map. Data is lost on restart;
 * a MongoDB adapter would implement the same two ports (see docs/adr/0004).
 */
@Component
public class InMemoryPartnerRepository implements LoadPartnerPort, SavePartnerPort {

    private final Map<PartnerId, BusinessPartner> partners = new ConcurrentHashMap<>();

    @Override
    public void save(BusinessPartner partner) {
        partners.put(partner.id(), partner);
    }

    @Override
    public Optional<BusinessPartner> findById(PartnerId id) {
        return Optional.ofNullable(partners.get(id));
    }

    @Override
    public List<BusinessPartner> findAll() {
        return query(partner -> true);
    }

    @Override
    public List<BusinessPartner> findByCountry(String country) {
        return query(partner -> country.equals(partner.data().address().country()));
    }

    @Override
    public List<BusinessPartner> findByIban(String iban) {
        return query(partner -> iban.equals(partner.data().iban()));
    }

    private List<BusinessPartner> query(Predicate<BusinessPartner> filter) {
        return partners.values().stream()
                .filter(filter)
                .sorted(Comparator.comparing(BusinessPartner::onboardedAt))
                .toList();
    }
}
