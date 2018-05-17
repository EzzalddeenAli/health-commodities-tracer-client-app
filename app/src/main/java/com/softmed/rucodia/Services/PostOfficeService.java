package com.softmed.rucodia.Services;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.google.gson.Gson;
import com.softmed.rucodia.api.Endpoints;
import com.softmed.rucodia.database.AppDatabase;
import com.softmed.rucodia.dom.objects.Transactions;
import com.softmed.rucodia.utils.ServiceGenerator;
import com.softmed.rucodia.utils.SessionManager;

import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



public class PostOfficeService extends IntentService {
    private static final String TAG = PostOfficeService.class.getSimpleName();

    AppDatabase database;
    SessionManager sess;
    Endpoints.TransactionServices transactionServices;

    public PostOfficeService() {
        super("PostOfficeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        database = AppDatabase.getDatabase(this.getApplicationContext());
        sess = new SessionManager(this.getApplicationContext());


        Log.d(TAG,"username = "+ sess.getUserName());
        Log.d(TAG,"password = "+ sess.getUserPass());


        transactionServices = ServiceGenerator.createService(Endpoints.TransactionServices.class,
                sess.getUserName(),
                sess.getUserPass());

        List<Transactions> transactions = database.transactionsDao().getUnPostedTransactions();

        for (final Transactions transaction : transactions) {

            Call call = transactionServices.postTransaction(getTransactionRequestBody(transaction));
            call.enqueue(new Callback() {
                @SuppressLint("StaticFieldLeak")
                @Override
                public void onResponse(Call call, Response response) {
                    //Store Received Patient Information, TbPatient as well as PatientAppointments
                    if (response.code() == 200 ||response.code() == 201) {
                        Log.d(TAG, "Successful Transaction responce " + response.body());
                        transaction.setSyncStatus(1);

                        new AsyncTask<Void, Void, Void>(){
                            @Override
                            protected Void doInBackground(Void... voids) {
                                database.transactionsDao().addTransactions(transaction);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                super.onPostExecute(aVoid);

                            }
                        }.execute();
                    } else {
                        Log.d(TAG, "Transaction Responce Call URL " + call.request().url());
                        Log.d(TAG, "Transaction Responce Code " + response.code());
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    Log.d(TAG,"PostOfficeService Error = "+ t.getMessage());
                    Log.d(TAG,"PostOfficeService CALL URL = "+ call.request().url());
                    Log.d(TAG,"PostOfficeService CALL Header = "+ call.request().header("Authorization"));
                }
            });
        }


        // Release the wake lock provided by the WakefulBroadcastReceiver.
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }

    public static RequestBody getTransactionRequestBody(Transactions transactions){

        RequestBody body;
        String datastream = "";

        try {
            datastream = new Gson().toJson(transactions);

            Log.d(TAG,"Transaction Object = "+datastream);

            body = RequestBody.create(MediaType.parse("application/json"), datastream);

        }catch (Exception e){
            e.printStackTrace();
            body = RequestBody.create(MediaType.parse("application/json"), datastream);
        }

        return body;

    }
}