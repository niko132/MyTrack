package de.mytrack.mytrackapp.ui.statistics.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Calendar;
import java.util.List;

import de.mytrack.mytrackapp.databinding.FragmentCalendarViewBinding;

public class CalendarViewFragment extends Fragment {

    private CalendarViewViewModel calendarViewViewModel;
    private FragmentCalendarViewBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        calendarViewViewModel = new ViewModelProvider(this).get(CalendarViewViewModel.class);

        binding = FragmentCalendarViewBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        calendarViewViewModel.getDayData().observe(getViewLifecycleOwner(), lists -> {
            if (lists.size() != 7)
                return;

            ViewGroup[] containers = new ViewGroup[]{
                    binding.calendarContainerMonday,
                    binding.calendarContainerTuesday,
                    binding.calendarContainerWednesday,
                    binding.calendarContainerThursday,
                    binding.calendarContainerFriday,
                    binding.calendarContainerSaturday,
                    binding.calendarContainerSunday
            };

            for (int i = 0; i < 7; i++) {
                containers[i].removeAllViews();

                List<CalendarViewViewModel.VisitedArea> dayAreas = lists.get(i);
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_WEEK, (i + 1) % 7 + 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long lastAreaLeaveMs = calendar.getTime().getTime();

                for (CalendarViewViewModel.VisitedArea visitedArea : dayAreas) {
                    long whiteSpaceMillis = visitedArea.mEnterMs - lastAreaLeaveMs;

                    if (whiteSpaceMillis > 0) {
                        // insert whitespace
                        new Bubble((int)(whiteSpaceMillis / 1000.0 / 60.0 * 0.75)).addTo(containers[i]);
                    }

                    // insert bubble
                    new Bubble((int)(visitedArea.mDurationMs / 1000.0 / 60.0 * 0.75), visitedArea.mArea.color).addTo(containers[i]);
                    lastAreaLeaveMs = visitedArea.mEnterMs + visitedArea.mDurationMs;
                }
            }
        });
    }

    private static class Bubble {

        private final int mSizePx;
        private final int mColor;
        private boolean mWhitespace;

        public Bubble(float durationMin) {
            this(durationMin, 0x00000000); // transparent
            mWhitespace = true;
        }

        public Bubble(float durationMin, int color) {
            mSizePx = (int) (durationMin * 2.0); // TODO: convert from minutes to px
            mColor = color;
            mWhitespace = false;
        }

        public void addTo(@NonNull ViewGroup container) {
            CardView cardView = new CardView(container.getContext());
            cardView.setRadius(30);
            cardView.setCardBackgroundColor(mColor);

            if (mWhitespace)
                cardView.setCardElevation(0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, mSizePx);
            cardView.setLayoutParams(params);

            container.addView(cardView);
        }

    }
}
