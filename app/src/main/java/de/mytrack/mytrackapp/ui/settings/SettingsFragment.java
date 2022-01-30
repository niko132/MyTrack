package de.mytrack.mytrackapp.ui.settings;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Arrays;
import java.util.List;

import de.mytrack.mytrackapp.MyApplication;
import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.BatteryDelayTime;
import de.mytrack.mytrackapp.databinding.FragmentSettingsBinding;
import de.mytrack.mytrackapp.export.Exporter;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    private ActivityResultLauncher<String> mCreateDocument;

    private AppDatabase mDatabase = MyApplication.appContainer.database;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCreateDocument = registerForActivityResult(
                new ActivityResultContracts.CreateDocument(), result -> {
                    if (result != null)
                        Exporter.saveDbToFile(result, getContext());
                }
        );
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.exportDataBtn.setOnClickListener(view1 ->
                mCreateDocument.launch("mytrack.txt")
        );

        int[] times = new int[]{2, 3, 4, 5, 10, 15, 20, 25, 30};
        String[] timesStrings = new String[times.length];
        for (int i = 0; i < times.length; i++) {
            timesStrings[i] = times[i] + " min.";
        }

        binding.normalBatteryTimePicker.setDisplayedValues(timesStrings);
        binding.normalBatteryTimePicker.setMinValue(0);
        binding.normalBatteryTimePicker.setMaxValue(times.length - 1);
        binding.normalBatteryTimePicker.setValue(0);
        binding.normalBatteryTimePicker.setWrapSelectorWheel(false);
        binding.normalBatteryTimePicker.setOnValueChangedListener((numberPicker, oldVal, newVal) -> {
            int timeInMin = times[newVal];
            AsyncTask.execute(() -> mDatabase.settingsDao().insertAll(BatteryDelayTime.normal(timeInMin)));
        });

        binding.lowBatteryTimePicker.setDisplayedValues(timesStrings);
        binding.lowBatteryTimePicker.setMinValue(0);
        binding.lowBatteryTimePicker.setMaxValue(times.length - 1);
        binding.lowBatteryTimePicker.setValue(0);
        binding.lowBatteryTimePicker.setWrapSelectorWheel(false);
        binding.lowBatteryTimePicker.setOnValueChangedListener((numberPicker, oldVal, newVal) -> {
            int timeInMin = times[newVal];
            AsyncTask.execute(() -> mDatabase.settingsDao().insertAll(BatteryDelayTime.low(timeInMin)));
        });

        binding.criticalBatteryTimePicker.setDisplayedValues(timesStrings);
        binding.criticalBatteryTimePicker.setMinValue(0);
        binding.criticalBatteryTimePicker.setMaxValue(times.length - 1);
        binding.criticalBatteryTimePicker.setValue(0);
        binding.criticalBatteryTimePicker.setWrapSelectorWheel(false);
        binding.criticalBatteryTimePicker.setOnValueChangedListener((numberPicker, oldVal, newVal) -> {
            int timeInMin = times[newVal];
            AsyncTask.execute(() -> mDatabase.settingsDao().insertAll(BatteryDelayTime.critical(timeInMin)));
        });

        // set the initial values
        AsyncTask.execute(() -> {
            int normalMin = mDatabase.settingsDao().getNormalDelayTime();
            int lowMin = mDatabase.settingsDao().getLowDelayTime();
            int criticalMin = mDatabase.settingsDao().getCriticalDelayTime();

            int normalIndex = 0;
            int lowIndex = 0;
            int criticalIndex = 0;

            for (int i = 0; i < times.length; i++) {
                if (times[i] == normalMin)
                    normalIndex = i;

                if (times[i] == lowMin)
                    lowIndex = i;

                if (times[i] == criticalMin)
                    criticalIndex = i;
            }

            int finalNormalIndex = normalIndex;
            int finalLowIndex = lowIndex;
            int finalCriticalIndex = criticalIndex;

            binding.getRoot().post(() -> {
                binding.normalBatteryTimePicker.setValue(finalNormalIndex);
                binding.lowBatteryTimePicker.setValue(finalLowIndex);
                binding.criticalBatteryTimePicker.setValue(finalCriticalIndex);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}