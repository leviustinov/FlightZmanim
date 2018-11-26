package flightzmanim;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;
import net.sf.geographiclib.GeodesicMask;
import net.sourceforge.zmanim.ComplexZmanimCalendar;
import net.sourceforge.zmanim.util.GeoLocation;
import net.sourceforge.zmanim.util.ZmanimFormatter;

public class Zmanim {
	public static void zmanimTest() {
		/*
		 * Some notes on lat/lon system (in decimal degrees): Latitude is X
		 * position on globe (right/left) and is measured with west/east, where
		 * west is negative and east is positive (-180 to 180)
		 * 
		 * Longitude is Y position on the glove (up/down) and is measured with
		 * south/north, where north is positive and south is negative (-90 to
		 * 90)
		 */

		/*
		 * Coordinates: JFK - 40.6413° N, 73.7781° W LHR - 51.4700° N, 0.4543° W
		 */

		// Date class needs to be formated for a specific time zone
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		String locationName = "Departure"; // (example)
		// based on departure (example)
		TimeZone timeZone = TimeZone.getTimeZone("America/New_York");
		// lets depart on 2016-06-15 00:32
		Calendar departure = new GregorianCalendar(2016, Calendar.JUNE, 15, 0, 32);
		double lat1 = 40.6413, lon1 = -73.7781, // JFK (example)
				lat2 = 51.4700, lon2 = -0.4543; // LHR (example)
		double elevation = 0; // mishor (on the plane)
		int flightDuration = 8 * 60; // in minutes (example)
		// Calculates shortest path on earth in meters between the coordinates
		GeodesicLine path = Geodesic.WGS84.InverseLine(lat1, lon1, lat2, lon2);
		int speed = (int) path.Distance() / flightDuration; // meters per minute

		// departure location
		GeoLocation location = new GeoLocation(locationName, lat1, lon1, elevation, timeZone);
		// zmanim object (just using departure locator to start out with
		ComplexZmanimCalendar zmanim = new ComplexZmanimCalendar(location);
		// time zone needs to be adjusted to the one we are using
		departure.setTimeZone(timeZone);

		// perform a binary search on the geodesic path until we find sunrise
		int min = 1, // lower bound of binary search
				max = (int) path.Distance(), // upper bound
				sunrisePoint = 0; // the distance point at which the sun will
									// rise
		// the search:
		while (min < max) {
			int mid = (min + max) / 2; // find the middle of the current search
										// area
			// check if we are not chasing ghosts...
			if (max - min == 1) {
				// we found all we need - take the upper bound and get out
				// (machmir on sunrise)
				sunrisePoint = max;
				break;
			}

			// adjust to current location (coordinates) to current distance
			// point on path
			GeodesicData g = path.Position(mid, GeodesicMask.LATITUDE | GeodesicMask.LONGITUDE);
			GeoLocation currentLocation = new GeoLocation(locationName, g.lat2, g.lon2, elevation, timeZone);
			zmanim.setGeoLocation(currentLocation);

			// adjust to current time to the time already spent in flight
			int timeOffset = (int) mid / speed; // how many minutes have we
												// traveled in this distance
			Calendar currentTime = (Calendar) departure.clone(); // get a copy
																	// of the
																	// departure
																	// date
			// not sure why I was not able to call the add method on the above
			// cast...
			currentTime.add(Calendar.MINUTE, timeOffset); // offset it to our
															// current position
			zmanim.setCalendar(currentTime); // pass the current time to the
												// zmanim calculator

			// retrieve sunrise for current point in time/distance from source
			Calendar sunrise = new GregorianCalendar(); // I want to use a
														// calendar object (and
														// not date)
			sunrise.setTime(zmanim.getSunrise()); // retrieve sunrise for
													// current time

			System.out.println("Current time: " + dateFormat.format(zmanim.getCalendar().getTime())
					+ ". Current sunrise: " + dateFormat.format(sunrise.getTime()));
			// System.out.println(g.lat2 + "," + g.lon2);

			// verify sunrise has not occurred yet
			if (sunrise.equals(currentTime)) {
				sunrisePoint = mid;
				break;
			} else if (sunrise.after(currentTime)) {
				// move up the minimum
				min = mid;
			} else { // must be before
				// move down the maximum
				max = mid;
			}
		}

		System.out.println(
				"Sunrise at " + sunrisePoint + " meters or " + sunrisePoint / speed + " minutes into the flight.");
		System.out.println(zmanim.getGeoLocation());

		GeoLocation currentLocation = new GeoLocation(locationName, 49.80094551871059, -52.46928011784894, 0, timeZone);
		zmanim.setGeoLocation(currentLocation);
		System.out.println("Sunrise at: " + dateFormat.format(zmanim.getSunrise()));

		// trying new formatter class
		ZmanimFormatter f = new ZmanimFormatter(timeZone);
		System.out.println(f.formatDateTime(zmanim.getSunrise(), zmanim.getCalendar()));
	}
}
