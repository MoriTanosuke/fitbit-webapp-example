import com.fitbit.api.FitbitAPIException;
import com.fitbit.api.client.*;
import com.fitbit.api.client.service.FitbitAPIClientService;
import com.fitbit.api.common.model.timeseries.Data;
import com.fitbit.api.common.model.timeseries.TimePeriod;
import com.fitbit.api.common.model.timeseries.TimeSeriesResourceType;
import com.fitbit.api.common.service.FitbitApiService;
import com.fitbit.api.model.APIResourceCredentials;
import com.fitbit.api.model.FitbitUser;
import org.joda.time.LocalDate;
import spark.*;
import spark.template.freemarker.FreeMarkerRoute;

import java.text.SimpleDateFormat;
import java.util.*;

import static spark.Spark.before;
import static spark.Spark.get;

public class HelloSpark {
    private static final String CONSUMER_KEY = System.getProperty("CONSUMER_KEY");
    private static final String CLIENT_SECRET = System.getProperty("CONSUMER_SECRET");
    private static final String BASE_URL = "http://127.0.0.1:4567";

    final FitbitApiCredentialsCache credentialsCache = new FitbitApiCredentialsCacheMapImpl();
    final FitbitAPIEntityCache entityCache = new FitbitApiEntityCacheMapImpl();
    final FitbitApiSubscriptionStorage subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
    final FitbitAPIClientService<FitbitApiClientAgent> fitbit = new FitbitAPIClientService<FitbitApiClientAgent>(
            new FitbitApiClientAgent("api.fitbit.com", "http://www.fitbit.com", credentialsCache),
            CONSUMER_KEY, CLIENT_SECRET, credentialsCache, entityCache, subscriptionStore);

    public static void main(String[] args) {
        new HelloSpark().init();
    }

    public void init() {
        // check if there is already a user object in session, otherwise log in
        before(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                boolean authenticated = request.session().attribute("user") != null;
                // if not authenticated and URL is something other than "login", redirect into login
                if (!authenticated && !request.url().contains("/login")) {
                    response.redirect("/login");
                }
            }
        });

        get(new FreeMarkerRoute("/") {
            @Override
            public ModelAndView handle(Request request, Response response) {
                Map<String, Object> attributes = new HashMap<String, Object>();
                String view = "index.ftl";

                // no memberSince available
                attributes.put("memberSince", "--");

                try {
                    final LocalUserDetail localUser = request.session().attribute("user");
                    final FitbitApiClientAgent client = request.session().attribute("client");
                    // overwrite memberSince
                    attributes.put("memberSince", client.getUserInfo(localUser).getMemberSince());
                } catch (FitbitAPIException e) {
                    attributes.put("error", e.getMessage());
                    view = "error.ftl";
                }

                return modelAndView(attributes, view);
            }
        });
        get(new Route("/login") {
            @Override
            public Object handle(Request request, Response response) {

                if (request.queryParams("completeAuthorization") != null) {
                    // Get temporary token and verifier returned by Fitbit from query string
                    String tempTokenReceived = request.queryParams("oauth_token");
                    String tempTokenVerifier = request.queryParams("oauth_verifier");

                    // Fetch user credentials from cache by temporary token from query string
                    APIResourceCredentials resourceCredentials = fitbit.getResourceCredentialsByTempToken(tempTokenReceived);

                    // Handle error when there is no record of credentials in cache for the temporary token provided
                    // As implementation of the credentials cache in this example is not persistant,
                    // this error will popup if you restart application, while user's browser will be on Fitbit
                    if (resourceCredentials == null) {
                        return "Oops, something went wrong. Temporary token is broken: " + tempTokenReceived;
                    }

                    // Call method of Fitbit4J to get token credentials only if necessary (they haven't been cached yet)
                    if (!resourceCredentials.isAuthorized()) {
                        // The verifier is required in the request to get token credentials
                        resourceCredentials.setTempTokenVerifier(tempTokenVerifier);
                        try {
                            // Get token credentials for the user
                            LocalUserDetail localUser = new LocalUserDetail(resourceCredentials.getLocalUserId());
                            fitbit.getTokenCredentials(localUser);
                            // set user into session
                            request.session().attribute("user", localUser);
                            request.session().attribute("client", fitbit.getClient());
                        } catch (FitbitAPIException e) {
                            return "Oops, something went wrong: " + e.getMessage();
                        }
                    }

                    // redirect to step chart
                    response.redirect("/steps");
                } else {
                    try {
                        // Fetch temporary credentials, construct redirect and serve it to user's browser
                        response.redirect(fitbit.getResourceOwnerAuthorizationURL(
                                new LocalUserDetail("-"),
                                BASE_URL + "/login?completeAuthorization="));
                    } catch (FitbitAPIException e) {
                        return "Oops, something went wrong: " + e.getMessage();
                    }
                }

                return "here comes the login";
            }
        });
        get(new FreeMarkerRoute("/steps") {
            @Override
            public ModelAndView handle(Request request, Response response) {

                final LocalUserDetail localUser = request.session().attribute("user");
                final FitbitApiClientAgent client = request.session().attribute("client");

                Map<String, Object> attributes = new HashMap<String, Object>();
                String view = "steps.ftl";
                try {
                    LocalDate startDate = FitbitApiService.getValidLocalDateOrNull(today());
                    List<Data> data = loadData(localUser, client, startDate, new FitbitUser("-"), TimeSeriesResourceType.STEPS, TimePeriod.ONE_WEEK);

                    attributes.put("data", data);
                } catch (FitbitAPIException e) {
                    attributes.put("error", e.getMessage());
                    view = "error.ftl";
                }
                return modelAndView(attributes, view);
            }
        });
    }

    private static List<Data> loadData(final LocalUserDetail localUser,
                                       FitbitApiClientAgent client, LocalDate startDate,
                                       FitbitUser fitbitUser, TimeSeriesResourceType type,
                                       TimePeriod period) throws FitbitAPIException {
        return client.getTimeSeries(localUser, fitbitUser, type, startDate, period);
    }

    private static String today() {
        return today(0);
    }

    private static String today(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, days);
        return today(cal.getTime());
    }

    private static String today(Date date) {
        return new SimpleDateFormat(FitbitApiService.LOCAL_DATE_PATTERN).format(date);
    }

}
