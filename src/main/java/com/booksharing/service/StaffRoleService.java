package com.booksharing.service;

import com.booksharing.entity.User;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.ListingRepository;
import com.booksharing.repository.RequestRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffRoleService {

    private final ListingRepository listingRepository;
    private final RequestRepository requestRepository;
    private final ExchangeRepository exchangeRepository;

    @Transactional(readOnly = true)
    public List<String> moderatorBlockers(User user) {
        if (user.getRole() != UserRole.USER) {
            return List.of();
        }
        List<String> blockers = new ArrayList<>();
        if (user.getStatus() != UserStatus.ACTIVE) {
            blockers.add("обліковий запис обмежено");
        }
        long listings = listingRepository.countByOwnerId(user.getId());
        if (listings > 0) {
            blockers.add(plural(listings, "оголошення", "оголошення", "оголошень"));
        }
        long requests = requestRepository.countByRequesterId(user.getId());
        if (requests > 0) {
            blockers.add(plural(requests, "заявка", "заявки", "заявок"));
        }
        long exchanges = exchangeRepository.countByOwnerIdOrReaderId(user.getId(), user.getId());
        if (exchanges > 0) {
            blockers.add(plural(exchanges, "обмін", "обміни", "обмінів"));
        }
        return blockers;
    }

    private static String plural(long n, String one, String few, String many) {
        long lastTwo = n % 100;
        long last = n % 10;
        String word;
        if (lastTwo >= 11 && lastTwo <= 14) {
            word = many;
        } else if (last == 1) {
            word = one;
        } else if (last >= 2 && last <= 4) {
            word = few;
        } else {
            word = many;
        }
        return n + " " + word;
    }
}
