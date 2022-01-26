package de.mytrack.mytrackapp.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.mytrack.mytrackapp.databinding.FragmentSettingsBinding;
import de.mytrack.mytrackapp.export.Exporter;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    private ActivityResultLauncher<String> mCreateDocument;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCreateDocument = registerForActivityResult(
                new ActivityResultContracts.CreateDocument(), result ->
                        Exporter.saveDbToFile(result, getContext())
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}