package com.rentitup.cron_service.constants;

public final class NotificationConstants {

	private NotificationConstants() {}

	public static final String EMAIL_CHANNEL = "EMAIL";
	public static final String SMS_CHANNEL = "SMS";
	public static final String PUSH_CHANNEL = "PUSH";

	public static final String LOW_PRIORITY = "LOW";
	public static final String NORMAL_PRIORITY = "NORMAL";
	public static final String HIGH_PRIORITY = "HIGH";
	public static final String URGENT_PRIORITY = "URGENT";

	public static final String BOOKING_CONFIRMED_TEMPLATE = "booking_confirmed";
	public static final String BOOKING_CANCELLED_TEMPLATE = "booking_cancelled";
	public static final String BOOKING_REMINDER_TEMPLATE = "booking_reminder";
	public static final String NEW_BOOKING_REQUEST_TEMPLATE = "new_booking_request";

	public static final String MAINTENANCE_REMINDER_TEMPLATE = "maintenance_reminder";

	public static final String REVIEW_REQUEST_TEMPLATE = "review_request";
	public static final String NEW_REVIEW_RECEIVED_TEMPLATE = "new_review_received";

	public static final String WELCOME_TEMPLATE = "welcome";
	public static final String PASSWORD_RESET_TEMPLATE = "password_reset";
	public static final String KYC_APPROVED_TEMPLATE = "kyc_approved";
	public static final String KYC_REJECTED_TEMPLATE = "kyc_rejected";

	public static final String PAYMENT_RECEIVED_TEMPLATE = "payment_received";
	public static final String PAYMENT_FAILED_TEMPLATE = "payment_failed";
	public static final String PAYOUT_SENT_TEMPLATE = "payout_sent";

	public static final String DATA_USER_NAME = "userName";
	public static final String DATA_OWNER_NAME = "ownerName";
	public static final String DATA_CUSTOMER_NAME = "customerName";
	public static final String DATA_MACHINE_NAME = "machineName";
	public static final String DATA_BOOKING_ID = "bookingId";
	public static final String DATA_START_DATE = "startDate";
	public static final String DATA_END_DATE = "endDate";
	public static final String DATA_TOTAL_AMOUNT = "totalAmount";
	public static final String DATA_PICKUP_LOCATION = "pickupLocation";
	public static final String DATA_NEXT_SERVICE_DATE = "nextServiceDate";
	public static final String DATA_LAST_SERVICE_DATE = "lastServiceDate";
	public static final String DATA_RATING = "rating";
	public static final String DATA_REVIEW_TEXT = "reviewText";
	public static final String DATA_REVIEW_LINK = "reviewLink";
	public static final String DATA_AMOUNT = "amount";
	public static final String DATA_PAYMENT_METHOD = "paymentMethod";
	public static final String DATA_REASON = "reason";
	public static final String DATA_RESET_LINK = "resetLink";
	public static final String DATA_EXPIRES_IN = "expiresIn";
	public static final String DATA_ACCOUNT_INFO = "accountInfo";
}
