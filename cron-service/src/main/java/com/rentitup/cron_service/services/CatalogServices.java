package com.rentitup.cron_service.services;

import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.booking.BookingServiceGrpc;
import com.rentitup.shared.proto.booking.Empty;
import com.rentitup.shared.proto.booking.ListReviewResponse;
import com.rentitup.shared.proto.booking.Review;
import com.rentitup.shared.proto.catalog.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;


@Service
@Slf4j
public class CatalogServices {

	@GrpcClient("CATALOG-SERVICE")
	private CatalogServiceGrpc.CatalogServiceBlockingStub catalogClient;

	@GrpcClient("BOOKING-SERVICE")
	private BookingServiceGrpc.BookingServiceBlockingStub bookingClient;

	public void UpdateMachineRatings() {
		log.info("Getting all the reviews from the booking service");
		Iterator<ListReviewResponse> reviewResponseIterator = bookingClient.getAllReviews(Empty.getDefaultInstance());

		while (reviewResponseIterator.hasNext()) {
			List<Review> reviewResponse = reviewResponseIterator.next().getReviewsList();
			log.info("Start the loop on the reviews ...");
			for (Review review : reviewResponse) {
				Machine machine = catalogClient.getMachine(GetMachineRequest.newBuilder()
						.setId(review.getMachineId()).build())
						.getMachine();

				int totalReviews = machine.getTotalReviews() +1;
				double averageRating = (machine.getAverageRating() + review.getMachineRating()) / totalReviews ;

				log.info("The average rating of the machine {} is  {}" , machine.getName(), averageRating);
				MachineResponse response = catalogClient.updateMachineRating(UpdateMachineRatingRequest.newBuilder()
								.setMachineId(review.getMachineId())
								.setNewAverageRating(averageRating)
								.setTotalReviews(totalReviews)
						.build());
				log.info("Machine of id {} and name {} has been updated the rating : {} ,totalReviews: {}",response.getMachine().getId(),response.getMachine().getName(),averageRating,totalReviews);
			}
		}

	}

	public void sendUpcomingMaintenanceEmails(){

	}



}
