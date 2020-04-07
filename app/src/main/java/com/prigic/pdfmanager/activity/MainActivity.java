package com.prigic.pdfmanager.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

import com.prigic.pdfmanager.BuildConfig;
import com.prigic.pdfmanager.R;
import com.prigic.pdfmanager.fragment.AddImagesFragment;
import com.prigic.pdfmanager.fragment.HistoryFragment;
import com.prigic.pdfmanager.fragment.HomeFragment;
import com.prigic.pdfmanager.fragment.ImageToPdfFragment;
import com.prigic.pdfmanager.fragment.InvertPdfFragment;
import com.prigic.pdfmanager.fragment.MergeFilesFragment;
import com.prigic.pdfmanager.fragment.PdfToImageFragment;
import com.prigic.pdfmanager.fragment.RemoveDuplicatePagesFragment;
import com.prigic.pdfmanager.fragment.RemovePagesFragment;
import com.prigic.pdfmanager.fragment.SettingsFragment;
import com.prigic.pdfmanager.fragment.SplitFilesFragment;
import com.prigic.pdfmanager.fragment.TextToPdfFragment;
import com.prigic.pdfmanager.fragment.ViewFilesFragment;
import com.prigic.pdfmanager.util.ThemeUtils;

import static com.prigic.pdfmanager.util.Constants.ACTION_MERGE_PDF;
import static com.prigic.pdfmanager.util.Constants.ACTION_SELECT_IMAGES;
import static com.prigic.pdfmanager.util.Constants.ACTION_TEXT_TO_PDF;
import static com.prigic.pdfmanager.util.Constants.ACTION_VIEW_FILES;
import static com.prigic.pdfmanager.util.Constants.ADD_IMAGES;
import static com.prigic.pdfmanager.util.Constants.ADD_PWD;
import static com.prigic.pdfmanager.util.Constants.BUNDLE_DATA;
import static com.prigic.pdfmanager.util.Constants.COMPRESS_PDF;
import static com.prigic.pdfmanager.util.Constants.EXTRACT_IMAGES;
import static com.prigic.pdfmanager.util.Constants.LAUNCH_COUNT;
import static com.prigic.pdfmanager.util.Constants.OPEN_SELECT_IMAGES;
import static com.prigic.pdfmanager.util.Constants.PDF_TO_IMAGES;
import static com.prigic.pdfmanager.util.Constants.REMOVE_PAGES;
import static com.prigic.pdfmanager.util.Constants.REMOVE_PWd;
import static com.prigic.pdfmanager.util.Constants.REORDER_PAGES;
import static com.prigic.pdfmanager.util.Constants.VERSION_NAME;
import static com.prigic.pdfmanager.util.DialogUtils.ADD_WATERMARK;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private NavigationView mNavigationView;
    private SharedPreferences mSharedPreferences;
    private boolean mDoubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setThemeApp(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.app_name, R.string.app_name);

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        initializeValues();

        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory",
                "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory",
                "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory",
                "com.fasterxml.aalto.stax.EventFactoryImpl");

        Fragment fragment = checkForAppShortcutClicked();

        handleReceivedImagesIntent(fragment);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int count = mSharedPreferences.getInt(LAUNCH_COUNT, 0);
        mSharedPreferences.edit().putInt(LAUNCH_COUNT, count + 1).apply();

        String versionName = mSharedPreferences.getString(VERSION_NAME, "");
        if (!versionName.equals(BuildConfig.VERSION_NAME)) {
            mSharedPreferences.edit().putString(VERSION_NAME, BuildConfig.VERSION_NAME).apply();
        }
        getRuntimePermissions();


    }

    @Override
    protected void onResume() {
        super.onResume();
        ActionBar actionBar = getSupportActionBar();
        actionBar.show();
    }

    private Fragment checkForAppShortcutClicked() {
        Fragment fragment = new HomeFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (getIntent().getAction() != null) {
            switch (Objects.requireNonNull(getIntent().getAction())) {
                case ACTION_SELECT_IMAGES:
                    fragment = new ImageToPdfFragment();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(OPEN_SELECT_IMAGES, true);
                    fragment.setArguments(bundle);
                    break;
                case ACTION_VIEW_FILES:
                    fragment = new ViewFilesFragment();
                    setDefaultMenuSelected(1);
                    break;
                case ACTION_TEXT_TO_PDF:
                    fragment = new TextToPdfFragment();
                    setDefaultMenuSelected(4);
                    break;
                case ACTION_MERGE_PDF:
                    fragment = new MergeFilesFragment();
                    setDefaultMenuSelected(2);
                    break;
                default:
                    fragment = new HomeFragment();
                    break;
            }
        }
        if (areImagesRecevied())
            fragment = new ImageToPdfFragment();

        fragmentManager.beginTransaction().replace(R.id.content, fragment).commit();

        return fragment;
    }

    private void initializeValues() {
        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        setDefaultMenuSelected(0);
    }

    public void setDefaultMenuSelected(int position) {
        if (mNavigationView != null && mNavigationView.getMenu() != null &&
                position < mNavigationView.getMenu().size()
                && mNavigationView.getMenu().getItem(position) != null) {
            mNavigationView.getMenu().getItem(position).setChecked(true);
        }
    }

    private void handleReceivedImagesIntent(Fragment fragment) {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (type == null || !type.startsWith("image/"))
            return;

        if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            handleSendMultipleImages(intent, fragment);
        } else if (Intent.ACTION_SEND.equals(action)) {
            handleSendImage(intent, fragment);
        }
    }


    private boolean areImagesRecevied() {
        Intent intent = getIntent();
        String type = intent.getType();
        return type != null && type.startsWith("image/");
    }

    private void handleSendImage(Intent intent, Fragment fragment) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        ArrayList<Uri> imageUris = new ArrayList<>();
        imageUris.add(uri);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(getString(R.string.bundleKey), imageUris);
        fragment.setArguments(bundle);
    }

    private void handleSendMultipleImages(Intent intent, Fragment fragment) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(getString(R.string.bundleKey), imageUris);
            fragment.setArguments(bundle);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.content);
            if (currentFragment instanceof HomeFragment) {
                checkDoubleBackPress();
            } else {
                Fragment fragment = new HomeFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();
                setDefaultMenuSelected(0);
            }
        }
    }

    private void checkDoubleBackPress() {
        if (mDoubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.mDoubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.confirm_exit_message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        Fragment fragment = null;
        FragmentManager fragmentManager = getSupportFragmentManager();
        Bundle bundle = new Bundle();

        switch (item.getItemId()) {
            case R.id.nav_home:
                fragment = new HomeFragment();
                break;
            case R.id.nav_camera:
                fragment = new ImageToPdfFragment();
                break;
            case R.id.nav_gallery:
                fragment = new ViewFilesFragment();
                break;
            case R.id.nav_merge:
                fragment = new MergeFilesFragment();
                break;
            case R.id.nav_split:
                fragment = new SplitFilesFragment();
                break;
            case R.id.nav_text_to_pdf:
                fragment = new TextToPdfFragment();
                break;
            case R.id.nav_history:
                fragment = new HistoryFragment();
                break;
            case R.id.nav_add_password:
                fragment = new RemovePagesFragment();
                bundle.putString(BUNDLE_DATA, ADD_PWD);
                fragment.setArguments(bundle);
                break;
            case R.id.nav_remove_password:
                fragment = new RemovePagesFragment();
                bundle.putString(BUNDLE_DATA, REMOVE_PWd);
                fragment.setArguments(bundle);
                break;
            case R.id.nav_settings:
                fragment = new SettingsFragment();
                break;
            case R.id.nav_extract_images:
                fragment = new PdfToImageFragment();
                bundle.putString(BUNDLE_DATA, EXTRACT_IMAGES);
                fragment.setArguments(bundle);
                break;
            case R.id.nav_pdf_to_images:
                fragment = new PdfToImageFragment();
                bundle.putString(BUNDLE_DATA, PDF_TO_IMAGES);
                fragment.setArguments(bundle);
                break;
            case R.id.nav_remove_pages:
                fragment = new RemovePagesFragment();
                bundle.putString(BUNDLE_DATA, REMOVE_PAGES);
                fragment.setArguments(bundle);
                break;
            case R.id.nav_rearrange_pages:
                fragment = new RemovePagesFragment();
                bundle.putString(BUNDLE_DATA, REORDER_PAGES);
                fragment.setArguments(bundle);
                break;
            case R.id.nav_compress_pdf:
                fragment = new RemovePagesFragment();
                bundle.putString(BUNDLE_DATA, COMPRESS_PDF);
                fragment.setArguments(bundle);
                break;
            case R.id.nav_add_images:
                fragment = new AddImagesFragment();
                bundle.putString(BUNDLE_DATA, ADD_IMAGES);
                fragment.setArguments(bundle);
                break;
            case R.id.nav_remove_duplicate_pages:
                fragment = new RemoveDuplicatePagesFragment();
                break;
            case R.id.nav_invert_pdf:
                fragment = new InvertPdfFragment();
                break;

            case R.id.nav_add_watermark:
                fragment = new ViewFilesFragment();
                bundle.putInt(BUNDLE_DATA, ADD_WATERMARK);
                fragment.setArguments(bundle);
                break;
        }

        try {
            if (fragment != null)
                fragmentManager.beginTransaction().replace(R.id.content, fragment).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void setNavigationViewSelection(int index) {
        mNavigationView.getMenu().getItem(index).setChecked(true);
    }

    private boolean getRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA},
                        0);
                return false;
            }
        }
        return true;
    }
}