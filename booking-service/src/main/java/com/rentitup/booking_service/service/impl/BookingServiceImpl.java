package com.rentitup.booking_service.service.impl;

import com.rentitup.booking_service.repository.BookingRepository;
import com.rentitup.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {
	private final BookingRepository bookingRepository;
}
