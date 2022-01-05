package de.mytrack.mytrackapp.ui.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;

import de.mytrack.mytrackapp.R;
import de.mytrack.mytrackapp.databinding.FragmentStatisticsBinding;
import de.mytrack.mytrackapp.ui.statistics.views.AreasViewFragment;
import de.mytrack.mytrackapp.ui.statistics.views.CalendarViewFragment;
import de.mytrack.mytrackapp.ui.statistics.views.MapViewFragment;

public class StatisticsFragment extends Fragment {

    private StatisticsViewModel statisticsViewModel;
    private FragmentStatisticsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        statisticsViewModel =
                new ViewModelProvider(this).get(StatisticsViewModel.class);

        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(statisticsViewModel);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        StatisticViewsAdapter adapter = new StatisticViewsAdapter(this);
        binding.viewPager.setAdapter(adapter);

        String[] tabTitles = getResources().getStringArray(R.array.statistics_tab_titles);
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,(tab, position) ->
                // TODO: maybe add proper error handling; for now an empty string should be ok
                tab.setText(position < tabTitles.length ? tabTitles[position] : "")).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class StatisticViewsAdapter extends FragmentStateAdapter {

        public StatisticViewsAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1:
                    return new AreasViewFragment();
                case 2:
                    return new MapViewFragment();
                default:
                    return new CalendarViewFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}