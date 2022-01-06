package de.mytrack.mytrackapp.ui.activities;

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import de.mytrack.mytrackapp.R;
import de.mytrack.mytrackapp.data.CustomActivity;
import de.mytrack.mytrackapp.databinding.FragmentActivitiesBinding;
import de.mytrack.mytrackapp.databinding.ItemCustomActivityBinding;

public class ActivitiesFragment extends Fragment {

    private ActivitiesViewModel activitiesViewModel;
    private FragmentActivitiesBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        activitiesViewModel =
                new ViewModelProvider(this).get(ActivitiesViewModel.class);

        binding = FragmentActivitiesBinding.inflate(inflater, container, false);
        binding.recyclerView.setAdapter(new CustomActivityAdapter());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.fab.setOnClickListener(view -> {
            final EditText nameEditText = new EditText(view.getContext());
            
            int[] colors = getResources().getIntArray(R.array.background_colors);
            final int color = colors[(int) (System.currentTimeMillis() % colors.length)];

            //
            new AlertDialog.Builder(view.getContext())
                    .setTitle(R.string.custom_activity_add_name)
                    .setView(nameEditText)
                    .setNegativeButton(R.string.custom_activity_add_cancel, (dialogInterface, i) ->
                            dialogInterface.dismiss())
                    .setPositiveButton(R.string.custom_activity_add_add, (dialogInterface, i) -> {
                        String name = nameEditText.getText().toString();
                        activitiesViewModel.onAddActivity(name, color);
                    })
                    .create()
                    .show();
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activitiesViewModel.getCustomActivities().observe(getViewLifecycleOwner(), customActivities -> {
            CustomActivityAdapter adapter = (CustomActivityAdapter) binding.recyclerView.getAdapter();
            if (adapter != null)
                adapter.setData(customActivities);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class CustomActivityAdapter extends RecyclerView.Adapter<CustomActivityAdapter.CustomActivityViewHolder> {

        private List<CustomActivity> mDataSet;

        class CustomActivityViewHolder extends RecyclerView.ViewHolder {
            final CardView background;
            final TextView nameTextView;
            final FloatingActionButton plusButton;
            final TextView lastClickedTextView;
            final TextView totalClickedTextView;

            public CustomActivityViewHolder(@NonNull ItemCustomActivityBinding binding) {
                super(binding.getRoot());

                this.background = binding.customActivityBackground;
                this.nameTextView = binding.customActivityName;
                this.plusButton = binding.customActivityPlusBtn;
                this.lastClickedTextView = binding.customActivityLastClicked;
                this.totalClickedTextView = binding.customActivityTotalClicked;
            }
        }

        void setData(@NonNull List<CustomActivity> data) {
            mDataSet = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CustomActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCustomActivityBinding customActivityBinding = ItemCustomActivityBinding
                    .inflate(LayoutInflater.from(parent.getContext()), parent, false);

            return new CustomActivityViewHolder(customActivityBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomActivityViewHolder holder, int position) {
            final CustomActivity activity = mDataSet.get(position);
            Context context = holder.background.getContext();

            String lastClickedText = context.getString(R.string.custom_activity_last,
                    DateUtils.getRelativeTimeSpanString(
                            activity.lastClickMs,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString());

            String totalClickedText = context.getString(R.string.custom_activity_total,
                    activity.totalClicks);

            holder.background.setCardBackgroundColor(activity.color);
            holder.nameTextView.setText(activity.name);
            holder.lastClickedTextView.setText(lastClickedText);
            holder.totalClickedTextView.setText(totalClickedText);

            holder.plusButton.setOnClickListener(view ->
                    activitiesViewModel.onActivityPlusClicked(activity));

            holder.background.setOnLongClickListener(view -> {
                activitiesViewModel.onActivityDeleteClicked(activity);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return mDataSet == null ? 0 : mDataSet.size();
        }
    }
}