package com.example.shopngo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        HomeFragment homeFragment = new HomeFragment();

        getSupportFragmentManager().beginTransaction().add(R.id.container, homeFragment).commit();
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.homenav:
                    HomeFragment homeFragmentnav = new HomeFragment();

                    getSupportFragmentManager().beginTransaction().replace(R.id.container, homeFragmentnav).commit();
                    return true;

                case R.id.offers:
                    OffersFragment offersFragment = new OffersFragment();

                    getSupportFragmentManager().beginTransaction().replace(R.id.container, offersFragment).commit();
                    return true;
                case R.id.cart:
                    CartFragment cartFragment = new CartFragment();

                    getSupportFragmentManager().beginTransaction().replace(R.id.container, cartFragment).commit();
                    return true;

                case R.id.account:
                    AccountFragment accountFragment = new AccountFragment();

                    getSupportFragmentManager().beginTransaction().replace(R.id.container, accountFragment).commit();
                    return true;


            }
            return false;
        });
    }

}