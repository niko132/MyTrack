package de.mytrack.mytrackapp.ui.statistics.views;

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.mytrack.mytrackapp.R;
import de.mytrack.mytrackapp.databinding.FragmentAreasViewBinding;
import de.mytrack.mytrackapp.databinding.ItemAreaDetailBinding;

public class AreasViewFragment extends Fragment {

    private AreasViewViewModel areasViewViewModel;
    private FragmentAreasViewBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        areasViewViewModel = new ViewModelProvider(this).get(AreasViewViewModel.class);

        binding = FragmentAreasViewBinding.inflate(inflater, container, false);
        binding.recyclerView.setAdapter(new AreaDetailAdapter());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        areasViewViewModel.getAreaDetailData().observe(getViewLifecycleOwner(), visitedAreaDetails -> {
            AreaDetailAdapter adapter = (AreaDetailAdapter) binding.recyclerView.getAdapter();
            if (adapter != null)
                adapter.setData(visitedAreaDetails);
        });
    }

    private static class AreaDetailAdapter extends RecyclerView.Adapter<AreaDetailAdapter.AreaDetailViewHolder> {

        private List<AreasViewViewModel.VisitedAreaDetail> mDataSet;

        static class AreaDetailViewHolder extends RecyclerView.ViewHolder {
            final CardView background;
            final TextView titleTextView;
            final TextView lastVisitedTextView;
            final TextView averageDurationTextView;

            public AreaDetailViewHolder(@NonNull ItemAreaDetailBinding binding) {
                super(binding.getRoot());

                this.background = binding.areaDetailBackground;
                this.titleTextView = binding.areaDetailTitle;
                this.lastVisitedTextView = binding.areaDetailLastVisited;
                this.averageDurationTextView = binding.areaDetailAverageDuration;
            }
        }

        void setData(@NonNull List<AreasViewViewModel.VisitedAreaDetail> data) {
            mDataSet = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AreaDetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAreaDetailBinding detailBinding = ItemAreaDetailBinding
                    .inflate(LayoutInflater.from(parent.getContext()), parent, false);

            return new AreaDetailViewHolder(detailBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull AreaDetailViewHolder holder, int position) {
            AreasViewViewModel.VisitedAreaDetail details = mDataSet.get(position);
            Context context = holder.background.getContext();

            String lastVisitedText = context.getString(R.string.area_detail_last,
                    DateUtils.getRelativeTimeSpanString(
                        details.mLastExitMs,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString());

            float averageMin = details.mAverageDurationMs / 1000.0f / 60.0f;
            float averageHours = averageMin / 60.0f;

            String averageDurationText;

            if (averageHours >= 1) {
                // use hours instead of minutes
                float hours = ((int)(averageHours * 2.0f + 0.5f)) / 2.0f;
                averageDurationText = context.getString(R.string.area_detail_average_hours, hours);
            } else {
                int min = (int)(averageMin + 0.5f);
                averageDurationText = context.getString(R.string.area_detail_average_min, min);
            }

            holder.background.setCardBackgroundColor(details.mArea.color);
            holder.titleTextView.setText(details.mArea.name);
            holder.lastVisitedTextView.setText(lastVisitedText);
            holder.averageDurationTextView.setText(averageDurationText);
        }

        @Override
        public int getItemCount() {
            return mDataSet == null ? 0 : mDataSet.size();
        }
    }
}
