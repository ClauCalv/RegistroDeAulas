package br.ufabc.gravador.views.activities;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import br.ufabc.gravador.R;
import br.ufabc.gravador.views.fragments.HelpFragment;
import br.ufabc.gravador.views.fragments.WelcomeFragment;
import br.ufabc.gravador.views.fragments.WelcomeFragment2;
import me.relex.circleindicator.CircleIndicator3;

public class HelpActivity extends AbstractMenuActivity {

    private static final int NUM_PAGES = 3;
    private ViewPager2 viewPager;
    private FragmentStateAdapter pagerAdapter;
    private CircleIndicator3 indicator;

    @Override
    protected int getLayoutID() {
        return R.layout.activity_help;
    }

    @Override
    protected void onSuperCreate(Bundle savedInstanceState) {
        menuHelp.setVisibility(View.INVISIBLE);
        menuConfig.setVisibility(View.INVISIBLE);

        viewPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        indicator = findViewById(R.id.indicator);
        indicator.setViewPager(viewPager);

        pagerAdapter.registerAdapterDataObserver(indicator.getAdapterDataObserver());
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0)
            super.onBackPressed();
        else
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
    }

    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 2:
                    return new HelpFragment();
                case 1:
                    return new WelcomeFragment2();
                case 0:
                default:
                    return new WelcomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }
}
