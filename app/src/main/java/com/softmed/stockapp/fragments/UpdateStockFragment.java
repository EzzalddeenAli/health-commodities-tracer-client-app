package com.softmed.stockapp.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.softmed.stockapp.R;
import com.softmed.stockapp.activities.MainActivity;
import com.softmed.stockapp.database.AppDatabase;
import com.softmed.stockapp.dom.dto.ProducToBeReportedtList;
import com.softmed.stockapp.dom.entities.Balances;
import com.softmed.stockapp.dom.entities.PostingFrequencies;
import com.softmed.stockapp.dom.entities.Product;
import com.softmed.stockapp.dom.entities.ProductReportingSchedule;
import com.softmed.stockapp.dom.entities.Transactions;
import com.softmed.stockapp.dom.entities.Unit;
import com.softmed.stockapp.utils.SessionManager;
import com.softmed.stockapp.workers.GetSchedulesWorker;
import com.softmed.stockapp.workers.SendTransactionsWorker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import fr.ganfra.materialspinner.MaterialSpinner;


/**
 * Dialog allowing users to select a date.
 */
public class UpdateStockFragment extends Fragment {
    private static final String TAG = UpdateStockFragment.class.getSimpleName();
    public static AppDatabase baseDatabase;
    private View dialogueLayout;
    private MaterialSpinner stockAdjustmentReasonSpinner, availabilityOfClientsOnRegimeSpinner;
    private TextInputLayout stockAdjustmentQuantity, numberOfClientsOnRegimeInputLayout, wastageInputLayout, quantityExpiredInputLayout, stockOutDaysInputLayout;
    private int productId;
    private String productName, scheduledDate;
    private int scheduledId;
    private boolean hasClients = false;
    private int stockQuantity;
    private Product product;
    private PostingFrequencies postingFrequency;
    private Unit unit;
    // Session Manager Class
    private SessionManager session;

    public UpdateStockFragment() {
    }


    public static UpdateStockFragment newInstance(ProducToBeReportedtList product) {
        UpdateStockFragment f = new UpdateStockFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("productId", product.getId());
        args.putString("productName", product.getName());
        args.putString("scheduledDate", product.getScheduledDate());
        args.putInt("scheduledId", product.getScheduleId());
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        final Activity activity = getActivity();
        baseDatabase = AppDatabase.getDatabase(getActivity());
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        productId = getArguments().getInt("productId");
        productName = getArguments().getString("productName");
        scheduledDate = getArguments().getString("scheduledDate");
        scheduledId = getArguments().getInt("scheduledId");
        session = new SessionManager(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        dialogueLayout = inflater.inflate(R.layout.fragment_update_stock, container, false);

        TextView productTitle = dialogueLayout.findViewById(R.id.product_name);
        productTitle.setText(productName);

        TextView scheduledDateTextView = dialogueLayout.findViewById(R.id.date);

        Date d = new Date();
        d.setTime(Long.parseLong(scheduledDate));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        scheduledDateTextView.setText("STOCK STATUS FOR " + dateFormat.format(d));

        stockAdjustmentQuantity = dialogueLayout.findViewById(R.id.stock_adjustment_quantity);
        numberOfClientsOnRegimeInputLayout = dialogueLayout.findViewById(R.id.number_of_clients_on_regime);
        quantityExpiredInputLayout = dialogueLayout.findViewById(R.id.quantity_expired);
        stockOutDaysInputLayout = dialogueLayout.findViewById(R.id.stock_out_days);
        wastageInputLayout = dialogueLayout.findViewById(R.id.wastage);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                product = baseDatabase.productsModelDao().getProductById(productId);
                postingFrequency = baseDatabase.postingFrequencyModelDao().getPostingFrequencyById(product.getPostingFrequency());

                Log.d(TAG, "Product = " + new Gson().toJson(product));
                unit = baseDatabase.unitsDao().getUnit(product.getUnitId());
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                stockAdjustmentQuantity.setHint("Stock On Hand in (" + unit.getName() + ")");

                if (product.isTrackWastage()) {
                    wastageInputLayout.setVisibility(View.VISIBLE);
                }

                if (product.isTrackQuantityExpired()) {
                    quantityExpiredInputLayout.setVisibility(View.VISIBLE);
                }

                if (product.isTrack_has_patients()) {
                    availabilityOfClientsOnRegimeSpinner.setVisibility(View.VISIBLE);
                }

                if (product.isTrackNumberOfPatients()) {
                    numberOfClientsOnRegimeInputLayout.setVisibility(View.VISIBLE);
                }

            }
        }.execute();


        stockAdjustmentReasonSpinner = dialogueLayout.findViewById(R.id.stock_adjustment_reason);
        availabilityOfClientsOnRegimeSpinner = dialogueLayout.findViewById(R.id.do_you_have_any_clients_on_regime);

        final String[] availabilityOfClientsOnRegime = {"Yes", "No"};
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(getActivity(), R.layout.simple_spinner_item_black, availabilityOfClientsOnRegime);
        spinAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_black);
        availabilityOfClientsOnRegimeSpinner.setAdapter(spinAdapter);
        availabilityOfClientsOnRegimeSpinner.setSelection(1);
        availabilityOfClientsOnRegimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    if (availabilityOfClientsOnRegime[i].equalsIgnoreCase("yes")) {
                        hasClients = true;

                        if (product.isTrackNumberOfPatients()) {
                            numberOfClientsOnRegimeInputLayout.setVisibility(View.VISIBLE);
                        }
                    } else {
                        hasClients = false;
                        if (product.isTrackNumberOfPatients()) {
                            numberOfClientsOnRegimeInputLayout.setVisibility(View.GONE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //Implement
            }
        });


        dialogueLayout.findViewById(R.id.add_transaction).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (checkInputs()) {
                    stockQuantity = 0;
                    try {
                        stockQuantity = Integer.valueOf(stockAdjustmentQuantity.getEditText().getText().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    new AsyncTask<Void, Void, String>() {
                        private int stockOutDays;
                        private String noOfClients, QuantityExpired, wastage;

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            stockOutDays = Integer.parseInt(stockOutDaysInputLayout.getEditText().getText().toString());
                            noOfClients = numberOfClientsOnRegimeInputLayout.getEditText().getText().toString();
                            QuantityExpired = quantityExpiredInputLayout.getEditText().getText().toString();
                            wastage = wastageInputLayout.getEditText().getText().toString();

                        }

                        @Override
                        protected String doInBackground(Void... voids) {
                            Transactions transactions = new Transactions();

                            transactions.setId(UUID.randomUUID().toString());
                            transactions.setProductId(productId);
                            transactions.setUserId(Integer.valueOf(session.getUserUUID()));
                            transactions.setTransactionTypeId(1);
                            transactions.setScheduleId(scheduledId);
                            transactions.setAmount(stockQuantity);
                            transactions.setHasClients(hasClients);
                            transactions.setSyncStatus(0);
                            transactions.setCreated_at(Calendar.getInstance().getTimeInMillis());
                            transactions.setStockOutDays(stockOutDays);

                            if (hasClients && product.isTrackNumberOfPatients()) {
                                transactions.setClientsOnRegime(noOfClients);
                            }

                            if (product.isTrackQuantityExpired()) {
                                transactions.setQuantityExpired(QuantityExpired);
                            }

                            if (product.isTrackWastage()) {
                                transactions.setWastage(wastage);
                            }

                            transactions.setStatusId(1);

                            Balances balances = baseDatabase.balanceModelDao().getBalance(productId, session.getFacilityId());
                            transactions.setConsumptionQuantity(balances.getConsumptionQuantity());
                            baseDatabase.transactionsDao().addTransactions(transactions);

                            balances.setBalance(stockQuantity);
                            baseDatabase.balanceModelDao().addBalance(balances);

                            try {
                                Log.d(TAG, "Updating product schedule");
                                ProductReportingSchedule reportingSchedule = baseDatabase.productReportingScheduleModelDao().getProductReportingScheduleById(scheduledId);
                                reportingSchedule.setStatus("posted");
                                baseDatabase.productReportingScheduleModelDao().addProductSchedule(reportingSchedule);
                                Log.d(TAG, "updated product schedule = " + new Gson().toJson(baseDatabase.productReportingScheduleModelDao().getProductReportingScheduleById(scheduledId)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return transactions.getId();
                        }

                        @Override
                        protected void onPostExecute(String transactionId) {
                            super.onPostExecute(transactionId);


                            // Create a Constraints object that defines when the task should run
                            Constraints networkConstraints = new Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build();


                            OneTimeWorkRequest sendTransactionWorker = new OneTimeWorkRequest.Builder(SendTransactionsWorker.class)
                                    .setConstraints(networkConstraints)
                                    .setInputData(
                                            new Data.Builder()
                                                    .putString("transactionId", transactionId)
                                                    .build()
                                    )
                                    .build();

                            OneTimeWorkRequest getPostingSchedule = new OneTimeWorkRequest.Builder(GetSchedulesWorker.class)
                                    .setConstraints(networkConstraints)
                                    .build();


                            WorkManager.getInstance()
                                    .beginWith(sendTransactionWorker)
                                    // Note: WorkManager.beginWith() returns a
                                    // WorkContinuation object; the following calls are
                                    // to WorkContinuation methods
                                    .then(getPostingSchedule)
                                    .enqueue();


                            ((MainActivity) getActivity()).moveToNextProduct();

                        }
                    }.execute();
                }

            }
        });


        return dialogueLayout;
    }

    public boolean checkInputs() {
        if (stockAdjustmentQuantity.getEditText().getText().toString().equals("")) {
            stockAdjustmentQuantity.getEditText().setError("Please fill the stock on hand quantity");
            return false;
        } else if (numberOfClientsOnRegimeInputLayout.getEditText().getText().toString().equals("") && product.isTrackNumberOfPatients() && hasClients) {
            numberOfClientsOnRegimeInputLayout.getEditText().setError("Please fill the number of clients on regime");
            return false;
        } else if (stockOutDaysInputLayout.getEditText().getText().toString().equals("")) {
            stockOutDaysInputLayout.getEditText().setError("Please fill the stockout days quantity");
            return false;
        } else if (Integer.parseInt(stockOutDaysInputLayout.getEditText().getText().toString()) > postingFrequency.getNumber_of_days()) {
            stockOutDaysInputLayout.getEditText().setError("Stock out days cannot be greater than " + postingFrequency.getNumber_of_days());
            return false;
        } else if (wastageInputLayout.getEditText().getText().toString().equals("") && product.isTrackWastage()) {
            wastageInputLayout.getEditText().setError("Please fill wastage quantity");
            return false;
        } else if (quantityExpiredInputLayout.getEditText().getText().toString().equals("") && product.isTrackQuantityExpired()) {
            quantityExpiredInputLayout.getEditText().setError("Please fill the quantity expired");
            return false;
        }
        return true;
    }

    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}
