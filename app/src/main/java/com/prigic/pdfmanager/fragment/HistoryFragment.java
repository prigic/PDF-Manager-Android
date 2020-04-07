package com.prigic.pdfmanager.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.prigic.pdfmanager.R;
import com.prigic.pdfmanager.activity.MainActivity;
import com.prigic.pdfmanager.adapter.HistoryAdapter;
import com.prigic.pdfmanager.util.AppDatabase;
import com.prigic.pdfmanager.util.History;
import com.prigic.pdfmanager.util.FileUtils;
import com.prigic.pdfmanager.util.ViewFilesDividerItemDecoration;

import static com.prigic.pdfmanager.util.DialogUtils.createWarningDialog;
import static com.prigic.pdfmanager.util.StringUtils.showSnackbar;

public class HistoryFragment extends Fragment implements HistoryAdapter.OnClickListener {

    @BindView(R.id.emptyStatusView)
    ConstraintLayout mEmptyStatusLayout;
    @BindView(R.id.historyRecyclerView)
    RecyclerView mHistoryRecyclerView;
    private Activity mActivity;
    private List<History> mHistoryList;
    private HistoryAdapter mHistoryAdapter;
    private boolean[] mFilterOptionState;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);
        ButterKnife.bind(this, root);

        mFilterOptionState = new boolean[getResources().getStringArray(R.array.filter_options_history).length];
        Arrays.fill(mFilterOptionState, Boolean.TRUE);
        new LoadHistory(mActivity).execute(new String[0]);
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_history_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionDeleteHistory:
                deleteHistory();
                break;
            case R.id.actionFilterHistory:
                openFilterDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        final String[] options = getResources().getStringArray(R.array.filter_options_history);

        builder.setMultiChoiceItems(options, mFilterOptionState, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index, boolean isChecked) {
                mFilterOptionState[index] = isChecked;
            }
        });

        builder.setTitle(getString(R.string.title_filter_history_dialog));

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ArrayList<String> selectedOptions = new ArrayList<>();
                for (int j = 0; j < mFilterOptionState.length; j++) {
                    if (mFilterOptionState[j]) {
                        selectedOptions.add(options[j]);
                    }
                }
                new LoadHistory(mActivity).execute(selectedOptions.toArray(new String[0]));
            }
        });

        builder.setNeutralButton(getString(R.string.select_all), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Arrays.fill(mFilterOptionState, Boolean.TRUE);
                new LoadHistory(mActivity).execute(new String[0]);
            }
        });
        builder.create().show();
    }

    private void deleteHistory() {
        MaterialDialog.Builder builder = createWarningDialog(mActivity,
                R.string.delete_history_message);
        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog2, @NonNull DialogAction which) {
                new DeleteHistory().execute();
            }
        })
                .show();
    }

    @OnClick(R.id.getStarted)
    public void loadHome() {
        Fragment fragment = new ImageToPdfFragment();
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content, fragment).commit();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setDefaultMenuSelected(0);
        }
    }

    @Override
    public void onItemClick(String path) {
        FileUtils fileUtils = new FileUtils(mActivity);
        File file = new File(path);
        if (file.exists()) {
            fileUtils.openFile(path);
        } else {
            showSnackbar(mActivity, R.string.pdf_does_not_exist_message);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadHistory extends AsyncTask<String[], Void, Void> {
        private final Context mContext;

        LoadHistory(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        protected Void doInBackground(String[]... args) {
            AppDatabase db = AppDatabase.getDatabase(mActivity.getApplicationContext());
            if (args[0].length == 0) {
                mHistoryList = db.historyDao().getAllHistory();
            } else {
                String[] filters = args[0];
                mHistoryList = db.historyDao().getHistoryByOperationType(filters);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mHistoryList != null && !mHistoryList.isEmpty()) {
                mEmptyStatusLayout.setVisibility(View.GONE);
                mHistoryAdapter = new HistoryAdapter(mActivity, mHistoryList, HistoryFragment.this);
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
                mHistoryRecyclerView.setLayoutManager(mLayoutManager);
                mHistoryRecyclerView.setAdapter(mHistoryAdapter);
                mHistoryRecyclerView.addItemDecoration(new ViewFilesDividerItemDecoration(mContext));
            } else {
                mEmptyStatusLayout.setVisibility(View.VISIBLE);
            }
            super.onPostExecute(aVoid);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class DeleteHistory extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase db = AppDatabase.getDatabase(mActivity.getApplicationContext());
            db.historyDao().deleteHistory();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mHistoryAdapter != null) {
                mHistoryAdapter.deleteHistory();
            }
            mEmptyStatusLayout.setVisibility(View.VISIBLE);
        }
    }
}