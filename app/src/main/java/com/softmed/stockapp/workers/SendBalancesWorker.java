package com.softmed.stockapp.workers;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.softmed.stockapp.api.Endpoints;
import com.softmed.stockapp.database.AppDatabase;
import com.softmed.stockapp.dom.entities.Balances;
import com.softmed.stockapp.utils.ServiceGenerator;
import com.softmed.stockapp.utils.SessionManager;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

import static com.softmed.stockapp.utils.AppUtils.getRequestBody;

/**
 * Created by cozej4 on 2019-07-18.
 *
 * @cozej4 https://github.com/cozej4
 */
public class SendBalancesWorker extends Worker {
    private static final String TAG = SendBalancesWorker.class.getSimpleName();


    public SendBalancesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        return sendBalances();
    }


    private Result sendBalances() {
        Log.d(TAG, "Send Balance Worker working");
        SessionManager sess = new SessionManager(this.getApplicationContext());
        Endpoints.TransactionServices transactionServices = ServiceGenerator.createService(Endpoints.TransactionServices.class,
                sess.getUserName(),
                sess.getUserPass());

        AppDatabase database = AppDatabase.getDatabase(this.getApplicationContext());
        List<Balances> balances = database.balanceModelDao().getUnPostedBalances();

        Log.d(TAG, "Sending Balance  " + new Gson().toJson(balances));

        Call postBalancesCall = transactionServices.postBalances(getRequestBody(balances));


        Response response = null;
        try {
            response = postBalancesCall.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response != null) {
            if (response.code() == 200 || response.code() == 201) {
                Log.d(TAG, "Successful Balance response " + new Gson().toJson(response.body()));
                for (Balances balance : balances) {
                    balance.setSyncStatus(1);
                    database.balanceModelDao().addBalance(balance);
                }

                return Result.success();
            } else if (response.code() != 500) {
                Log.d(TAG, "Balance  Call URL " + postBalancesCall.request().url());
                Log.d(TAG, "Balance  Code " + response.code());

                return Result.retry();
            } else {
                Log.d(TAG, "Posting Balance  Failed with code " + response.code());
                return Result.failure();
            }
        } else {
            return Result.failure();
        }
    }
}