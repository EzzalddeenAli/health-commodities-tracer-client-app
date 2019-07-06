package com.softmed.stockapp.api;

import androidx.room.Transaction;

import com.softmed.stockapp.Dom.entities.Balances;
import com.softmed.stockapp.Dom.entities.Location;
import com.softmed.stockapp.Dom.entities.Product;
import com.softmed.stockapp.Dom.entities.ProductReportingSchedule;
import com.softmed.stockapp.Dom.entities.TransactionType;
import com.softmed.stockapp.Dom.entities.Transactions;
import com.softmed.stockapp.Dom.responces.CategoriesResponse;
import com.softmed.stockapp.Dom.responces.LoginResponse;
import com.softmed.stockapp.Dom.responces.ProductReportingScheduleResponse;
import com.softmed.stockapp.Dom.responces.ProductsPostResponse;
import com.softmed.stockapp.Dom.responces.UnitsResponse;

import java.util.List;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Url;


public class Endpoints {

    private String HFUUID = "";

    public interface LoginService {
        @POST("rest-auth/login/")
        Call<LoginResponse> basicLogin(@Body RequestBody e);


        @GET("api_locations/")
        Call<List<Location>> getLocations();

        @GET("users")
        Call<List<LoginResponse>> getAllUsers();

    }

    public interface CategoriesService {
        @GET("api_health_commodity_category")
        Call<List<CategoriesResponse>> getCategories();

    }

    public interface ProductsService {
        @GET("api_health_commodity")
        Call<List<Product>> getProducts();

        @GET("api_unit")
        Call<List<UnitsResponse>> getUnits();

        @POST("products")
        Call<ProductsPostResponse> postProducts(@Body RequestBody e);
    }

    public interface TransactionServices {
        @GET("api_health_commodity_transactions")
        Call<List<Transactions>> getTransactions();

        @GET
        Call<List<Balances>> getBalances(@Url String url);

        @GET("api_posting_schedule")
        Call<List<ProductReportingScheduleResponse>> getSchedule();

        @GET("transactiontypes")
        Call<List<TransactionType>> getTransactionTypes();

        @POST("api_health_commodity_transactions/")
        Call<List<Transactions>> postTransaction(@Body RequestBody e);


        @POST("api_health_commodity_mapping/")
        Call<List<ProductReportingScheduleResponse>> postBalances(@Body RequestBody e);
    }

    public interface NotificationServices {
        @POST("save-push-notification-token")
        Call<String> registerDevice(@Body RequestBody u);
    }


}
