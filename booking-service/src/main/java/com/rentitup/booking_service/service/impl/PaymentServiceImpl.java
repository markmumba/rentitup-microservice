package com.rentitup.booking_service.service.impl;

import com.rentitup.booking_service.repository.PaymentRepository;
import com.rentitup.booking_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
	private  final PaymentRepository paymentRepository;
}
