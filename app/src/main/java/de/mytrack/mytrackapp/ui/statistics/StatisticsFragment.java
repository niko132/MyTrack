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

import de.mytrack.mytrackapp.databinding.FragmentDemoBinding;
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

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> tab.setText("Tab #" + (position + 1))).attach();
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
            switch(position) {
                case 0:
                    return new CalendarViewFragment();
                case 1:
                    return new AreasViewFragment();
                case 2:
                    return new MapViewFragment();
            }

            // TODO: remove
            Fragment demoFragment = new DemoFragment();
            Bundle args = new Bundle();
            args.putInt("aaa", position + 1);
            demoFragment.setArguments(args);

            return demoFragment;
        }

        @Override
        public int getItemCount() {
            return 10;
        }
    }

    public static class DemoFragment extends Fragment {

        private FragmentDemoBinding binding;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = FragmentDemoBinding.inflate(inflater, container, false);

            return binding.getRoot();
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Bundle args = getArguments();
            binding.textDashboard.setText(Integer.toString(args.getInt("aaa")));
        }
    }
}