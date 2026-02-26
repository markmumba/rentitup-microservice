package com.rentitup.booking_service.service.impl;

import com.rentitup.booking_service.repository.ReviewRepository;
import com.rentitup.booking_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {
	private final ReviewRepository reviewRepository;
}
