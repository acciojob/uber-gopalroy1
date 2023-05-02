package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.CabRepository;
import com.driver.services.CustomerService;
import org.assertj.core.internal.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.driver.repository.CustomerRepository;
import com.driver.repository.DriverRepository;
import com.driver.repository.TripBookingRepository;

import java.util.ArrayList;
import java.util.List;

import static com.driver.model.TripStatus.CANCELED;
import static com.driver.model.TripStatus.CONFIRMED;

@Service
public class CustomerServiceImpl implements CustomerService {

	@Autowired
	CustomerRepository customerRepository2;

	@Autowired
	DriverRepository driverRepository2;

	@Autowired
	CabRepository cabRepository2;

	@Autowired
	TripBookingRepository tripBookingRepository2;

	@Override
	public void register(Customer customer) {
		//Save the customer in database
		customerRepository2.save(customer);
	}

	@Override
	public void deleteCustomer(Integer customerId) {
		// Delete customer without using deleteById function
		Customer customer = customerRepository2.findById(customerId).get();
		customerRepository2.delete(customer);
	}

	@Override
	public TripBooking bookTrip(int customerId, String fromLocation, String toLocation, int distanceInKm) throws Exception{
		//Book the driver with lowest driverId who is free (cab available variable is Boolean.TRUE). If no driver is available, throw "No cab available!" exception
		//Avoid using SQL query
		List<Driver> driverList = driverRepository2.findAll();
		Driver driverBook = new Driver();
		driverBook.setDriverId(Integer.MAX_VALUE);
		boolean found=false;
		for (Driver d :driverList
			 ) {
			if (d.getCab().getAvailable()==true){
				if (d.getDriverId()<=driverBook.getDriverId()){
					driverBook=d;
					found= true;
				}
			}
		}
		if (found==false){
			 throw new Exception("No cab available!") ;
		}


		int totalAmount = driverBook.getCab().getPerKmRate()*distanceInKm;
		TripBooking tripBooking = new TripBooking(fromLocation,toLocation,distanceInKm,CONFIRMED,totalAmount);
		Customer customer = customerRepository2.findById(customerId).get();
		tripBooking.setCustomer(customer);
		customerRepository2.save(customer);
		Cab cab = driverBook.getCab();
		cab.setAvailable(false);
		tripBooking.setDriver(driverBook);
		driverBook.getTripBookingList().add(tripBooking);
		driverRepository2.save(driverBook);
		cabRepository2.save(driverBook.getCab());
		return tripBooking;

	}

	@Override
	public void cancelTrip(Integer tripId){
		//Cancel the trip having given trip Id and update TripBooking attributes accordingly
		TripBooking tripbooked = tripBookingRepository2.findById(tripId).get();
		tripbooked.setStatus(CANCELED);
		Cab cab = tripbooked.getDriver().getCab();
		cab.setAvailable(true);
		cabRepository2.save(cab);
		tripBookingRepository2.save(tripbooked);
	}

	@Override
	public void completeTrip(Integer tripId){
		//Complete the trip having given trip Id and update TripBooking attributes accordingly
		TripBooking tripbooked = tripBookingRepository2.findById(tripId).get();
		int total = tripbooked.getDistanceInKm()*tripbooked.getDriver().getCab().getPerKmRate();
		tripbooked.setBill(total);
		tripbooked.setStatus(TripStatus.COMPLETED);
		tripbooked.getDriver().getCab().setAvailable(true);
		cabRepository2.save(tripbooked.getDriver().getCab());
		tripBookingRepository2.save(tripbooked);

	}
}
